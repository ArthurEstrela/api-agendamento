package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IUserRepository userRepository;
    private final IServiceProviderRepository serviceProviderRepository;
    private final ICouponRepository couponRepository;
    private final ApplyCouponUseCase applyCouponUseCase;
    private final IEventPublisher eventPublisher;

    // ✨ Componentes para Concorrência e Transação Programática
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;

    public Appointment execute(CreateAppointmentInput input) {

        // 1. Definição da Chave de Lock (Escopo: O Profissional que receberá o agendamento)
        // Garante fila única para este profissional em ambiente distribuído.
        String lockKey = "lock:appointment:professional:" + input.professionalId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // 2. Tenta adquirir o lock distribuído
            // waitTime: 2s (espera na fila) | leaseTime: 5s (solta se o servidor morrer)
            boolean isLocked = lock.tryLock(2, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                // Fail-fast: Protege o sistema em picos de acesso
                throw new BusinessException("A agenda deste profissional está sendo atualizada. Tente novamente em instantes.");
            }

            try {
                // 3. Execução Transacional (Dentro do Lock)
                // TransactionTemplate garante que a transação inicie APÓS o lock e commite ANTES do unlock.
                TransactionTemplate template = new TransactionTemplate(transactionManager);
                return template.execute(status -> executeInTransaction(input));

            } finally {
                // Sempre libera o lock, sucesso ou erro
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Operação interrompida inesperadamente.");
        }
    }

    // Lógica de negócio pura, executada dentro da transação segura
    protected Appointment executeInTransaction(CreateAppointmentInput input) {

        // 1. Busca Profissional COM LOCK PESSIMISTA (SELECT ... FOR UPDATE)
        // Isso trava a linha do profissional no banco para esta transação.
        Professional professional = professionalRepository.findByIdWithLock(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Validações de Cliente
        User client = userRepository.findById(input.clientId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));

        // 3. Validação de Serviços
        List<Service> requestedServices = serviceRepository.findAllByIds(input.serviceIds());
        if (requestedServices.isEmpty()) {
            throw new BusinessException("Selecione ao menos um serviço.");
        }

        // 4. Validação de Competência e Horário
        professional.validateCanPerform(requestedServices);

        int totalDuration = requestedServices.stream().mapToInt(Service::getDuration).sum();
        
        // Calcula o preço base (soma dos serviços)
        BigDecimal basePrice = requestedServices.stream()
                .map(Service::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (!professional.isAvailable(input.startTime(), totalDuration)) {
            throw new BusinessException("Profissional indisponível neste horário (fora do expediente ou pausa).");
        }

        // 5. Lógica de Cupom de Desconto (✨ NOVO)
        Coupon coupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (input.couponCode() != null && !input.couponCode().isBlank()) {
            var result = applyCouponUseCase.validateAndCalculate(
                    input.couponCode(), 
                    professional.getServiceProviderId(), 
                    basePrice
            );
            coupon = result.coupon();
            discountAmount = result.discountAmount();
        }

        // Calcula preço final (nunca negativo)
        BigDecimal finalPrice = basePrice.subtract(discountAmount).max(BigDecimal.ZERO);

        // 6. Proteção contra Double Booking (Check Final no Banco)
        // Seguro devido ao Lock Pessimista no Profissional
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                input.professionalId(),
                input.startTime(),
                input.startTime().plusMinutes(totalDuration));

        if (hasConflict) {
            throw new ScheduleConflictException("Este horário acabou de ser ocupado por outro cliente.");
        }

        // 7. Recuperação do TimeZone
        String timeZone = serviceProviderRepository.findById(professional.getServiceProviderId())
                .map(ServiceProvider::getTimeZone)
                .orElse("America/Sao_Paulo");

        // 8. Criação da Entidade
        // Cria o objeto base usando o Factory Method e enriquece com dados financeiros via Builder
        Appointment appointment = Appointment.create(
                        client.getId(),
                        client.getName(),
                        client.getEmail(),
                        professional.getServiceProviderName(),
                        new ClientPhone(client.getPhoneNumber()),
                        professional.getServiceProviderId(),
                        professional.getId(),
                        professional.getName(),
                        requestedServices,
                        input.startTime(),
                        input.reminderMinutes(),
                        timeZone)
                .toBuilder() // ✨ Enriquece com dados financeiros
                .price(finalPrice) // Valor final a ser cobrado
                .couponId(coupon != null ? coupon.getId() : null)
                .discountAmount(discountAmount)
                .build();

        // 9. Persistência e Atualização de Cupom
        if (coupon != null) {
            coupon.incrementUsage();
            couponRepository.save(coupon); // Salva incremento de uso na mesma transação
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado com segurança (ID: {}, Valor: {}).", savedAppointment.getId(), finalPrice);

        // 10. Publicação do Evento
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedAppointment.getId(),
                professional.getId(),
                client.getName(),
                savedAppointment.getStartTime()));

        return savedAppointment;
    }

    public record CreateAppointmentInput(
            String clientId,
            String professionalId,
            List<String> serviceIds,
            LocalDateTime startTime,
            Integer reminderMinutes,
            String couponCode // ✨ Novo campo opcional
    ) {}
}