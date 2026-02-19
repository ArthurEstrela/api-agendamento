package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
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
import java.util.UUID;
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
    private final IEventPublisher eventPublisher;

    // Componentes para Concorrência Distribuída e Transação Programática
    private final RedissonClient redissonClient;
    private final PlatformTransactionManager transactionManager;

    public Appointment execute(Input input) {

        // 1. Lock Distribuído (Escopo: Profissional)
        // Garante que apenas uma requisição por vez tente agendar para este profissional.
        String lockKey = "lock:appointment:professional:" + input.professionalId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Tenta adquirir o lock: espera até 2s, segura por 5s
            boolean isLocked = lock.tryLock(2, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("Concorrência detectada para o profissional {}", input.professionalId());
                throw new BusinessException("A agenda deste profissional está sendo atualizada por outro usuário. Tente novamente.");
            }

            try {
                // 2. Execução Transacional Programática
                // Garante que a transação de banco de dados ocorra estritamente DENTRO do tempo do Lock Distribuído.
                TransactionTemplate template = new TransactionTemplate(transactionManager);
                return template.execute(status -> executeInTransaction(input));

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Operação interrompida inesperadamente.");
        }
    }

    // Lógica de negócio protegida por transação e lock
    protected Appointment executeInTransaction(Input input) {

        // 1. Busca Profissional (Lock Pessimista no Banco para garantir leitura consistente)
        Professional professional = professionalRepository.findByIdWithLock(input.professionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 2. Validações de Entidades Relacionadas
        User clientUser = userRepository.findById(input.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado."));

        ServiceProvider provider = serviceProviderRepository.findById(professional.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        if (!provider.isSubscriptionActive()) {
            throw new BusinessException("O estabelecimento não possui uma assinatura ativa para receber agendamentos.");
        }

        // 3. Validação e Busca de Serviços
        List<Service> requestedServices = serviceRepository.findAllByIds(input.serviceIds());
        
        if (requestedServices.isEmpty()) {
            throw new BusinessException("Selecione ao menos um serviço.");
        }
        
        if (requestedServices.size() != input.serviceIds().size()) {
            throw new BusinessException("Um ou mais serviços selecionados não foram encontrados ou estão inativos.");
        }

        // ✨ Segurança: Tenant Isolation
        // Garante que os serviços pertençam ao mesmo estabelecimento do profissional
        boolean servicesBelongToProvider = requestedServices.stream()
                .allMatch(s -> s.getServiceProviderId().equals(provider.getId()));
        
        if (!servicesBelongToProvider) {
            throw new BusinessException("Erro de consistência: Serviços não pertencem ao estabelecimento do profissional.");
        }

        // 4. Validação de Competência e Disponibilidade
        professional.validateCanPerform(requestedServices);

        int totalDuration = requestedServices.stream().mapToInt(Service::getDuration).sum();
        
        // Verifica se o profissional trabalha nesse horário e se cabe na grade
        if (!professional.isAvailable(input.startTime(), totalDuration)) {
            throw new BusinessException("Profissional indisponível neste horário (fora do expediente ou pausa).");
        }

        // 5. Cálculo Financeiro e Cupons
        BigDecimal basePrice = requestedServices.stream()
                .map(Service::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = null;
        BigDecimal discountAmount = BigDecimal.ZERO;

        if (input.couponCode() != null && !input.couponCode().isBlank()) {
            // Lógica de Cupom Inline para aproveitar o contexto transacional
            coupon = couponRepository.findByCodeAndProviderId(input.couponCode(), provider.getId())
                    .orElseThrow(() -> new BusinessException("Cupom inválido ou não encontrado."));
            
            // O método calculateDiscount do domínio já valida validade, uso e valor mínimo
            discountAmount = coupon.calculateDiscount(basePrice);
        }

        BigDecimal finalPrice = basePrice.subtract(discountAmount).max(BigDecimal.ZERO);

        // 6. Check Final de Conflito (Double Booking)
        // Verifica se já existe agendamento overlapping no banco
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                input.professionalId(),
                input.startTime(),
                input.startTime().plusMinutes(totalDuration));

        if (hasConflict) {
            throw new ScheduleConflictException("Desculpe, este horário acabou de ser ocupado por outro cliente.");
        }

        // 7. Construção da Entidade Appointment (Usando Factory do Domínio)
        Appointment appointment = Appointment.create(
                clientUser.getId(),
                clientUser.getName(),
                clientUser.getEmail(),
                provider.getBusinessName(),
                new ClientPhone(clientUser.getPhoneNumber()),
                provider.getId(),
                professional.getId(),
                professional.getName(),
                requestedServices,
                input.startTime(),
                input.reminderMinutes(),
                provider.getTimeZone() // Usa o fuso horário configurado no estabelecimento
        );

        // Enriquecimento com dados financeiros e opcionais
        appointment = appointment.toBuilder()
                .price(basePrice)         // Preço original
                .finalPrice(finalPrice)   // Preço com desconto
                .couponId(coupon != null ? coupon.getId() : null)
                .discountAmount(discountAmount)
                .notes(input.notes())
                .build();

        // 8. Persistência
        // Se houve uso de cupom, incrementa o uso
        if (coupon != null) {
            coupon.incrementUsage();
            couponRepository.save(coupon);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado com sucesso. ID: {}", savedAppointment.getId());

        // 9. Publicação de Evento (Assíncrono)
        // Dispara notificações (Push, Email) e atualizações de Dashboard
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedAppointment.getId(),
                professional.getId(),
                clientUser.getName(),
                savedAppointment.getStartTime()
        ));

        return savedAppointment;
    }

    // Input Record atualizado com UUIDs
    public record Input(
            UUID clientId,
            UUID professionalId,
            List<UUID> serviceIds,
            LocalDateTime startTime,
            Integer reminderMinutes,
            String couponCode,
            String notes // Campo útil para observações do cliente
    ) {}
}