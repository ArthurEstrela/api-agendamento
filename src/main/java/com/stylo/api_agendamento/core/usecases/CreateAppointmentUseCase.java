package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
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
        private final IEventPublisher eventPublisher;

        // ✨ Componentes para Concorrência e Transação Programática
        private final RedissonClient redissonClient;
        private final PlatformTransactionManager transactionManager;

        public Appointment execute(CreateAppointmentInput input) {

                // 1. Definição da Chave de Lock (Escopo: O Profissional que receberá o
                // agendamento)
                // Isso garante fila única para este profissional, independente de quantas
                // instâncias da API existam.
                String lockKey = "lock:appointment:professional:" + input.professionalId();
                RLock lock = redissonClient.getLock(lockKey);

                try {
                        // 2. Tenta adquirir o lock distribuído
                        // waitTime: 2s (espera na fila) | leaseTime: 5s (solta se o servidor morrer)
                        boolean isLocked = lock.tryLock(2, 5, TimeUnit.SECONDS);

                        if (!isLocked) {
                                // Fail-fast: Protege o sistema em picos de acesso
                                throw new BusinessException(
                                                "A agenda deste profissional está sendo atualizada. Tente novamente em instantes.");
                        }

                        try {
                                // 3. Execução Transacional (Dentro do Lock)
                                // Usamos TransactionTemplate para garantir que a transação inicie APÓS o lock
                                // e commite ANTES de soltarmos o lock.
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

                // 1. Busca Profissional (Agora findById simples, pois o Redis já protege a
                // concorrência)
                Professional professional = professionalRepository.findById(input.professionalId())
                                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

                // 2. Validações de Cliente
                User client = userRepository.findById(input.clientId())
                                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));

                // 3. Validação de Serviços
                List<Service> requestedServices = serviceRepository.findAllByIds(input.serviceIds());
                if (requestedServices.isEmpty()) {
                        throw new BusinessException("Selecione ao menos um serviço.");
                }

                // 4. Validação de Competência e Horário (Regras de Domínio em Memória)
                professional.validateCanPerform(requestedServices);

                int totalDuration = requestedServices.stream().mapToInt(Service::getDuration).sum();

                if (!professional.isAvailable(input.startTime(), totalDuration)) {
                        throw new BusinessException(
                                        "Profissional indisponível neste horário (fora do expediente ou pausa).");
                }

                // 5. Proteção contra Double Booking (Check Final no Banco)
                // Como estamos dentro do Lock do Redis, temos certeza absoluta que ninguém mais
                // está agendando neste slot.
                boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                                input.professionalId(),
                                input.startTime(),
                                input.startTime().plusMinutes(totalDuration));

                if (hasConflict) {
                        throw new ScheduleConflictException("Este horário acabou de ser ocupado por outro cliente.");
                }

                // 6. Recuperação do TimeZone
                String timeZone = serviceProviderRepository.findById(professional.getServiceProviderId())
                                .map(ServiceProvider::getTimeZone)
                                .orElse("America/Sao_Paulo");

                // 7. Criação da Entidade
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
                                timeZone);

                // 8. Persistência
                Appointment savedAppointment = appointmentRepository.save(appointment);
                log.info("Agendamento criado com segurança (ID: {}). TimeZone: {}", savedAppointment.getId(), timeZone);

                // 9. Publicação do Evento (Transação será commitada logo após este retorno)
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
                        Integer reminderMinutes) {
        }
}