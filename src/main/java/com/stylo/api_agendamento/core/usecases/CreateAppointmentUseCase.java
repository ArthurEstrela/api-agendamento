package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException;
import com.stylo.api_agendamento.core.ports.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IUserRepository userRepository;
    
    // ✨ Mudança: Agora dependemos apenas de um publicador de eventos,
    // desacoplando o Core das ferramentas externas (Google/Firebase).
    private final IEventPublisher eventPublisher;

    /**
     * Executa a criação do agendamento com concorrência segura.
     * O @Transactional garante que o Lock Pessimista dure até o return.
     */
    @Transactional
    public Appointment execute(CreateAppointmentInput input) {
        
        // 1. Busca Profissional COM LOCK (Pessimistic Locking)
        // Neste momento, se outro cliente tentar agendar para este mesmo profissional,
        // ele ficará esperando no banco de dados até esta transação terminar.
        Professional professional = professionalRepository.findByIdWithLock(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Validações Básicas
        User client = userRepository.findById(input.clientId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));

        List<Service> requestedServices = serviceRepository.findAllByIds(input.serviceIds());
        if (requestedServices.isEmpty()) {
            throw new BusinessException("Selecione ao menos um serviço.");
        }

        // 3. Validação de Competência e Horário de Trabalho
        professional.validateCanPerform(requestedServices);

        int totalDuration = requestedServices.stream().mapToInt(Service::getDuration).sum();
        if (!professional.isAvailable(input.startTime(), totalDuration)) {
            throw new BusinessException("Profissional indisponível neste horário (fora do expediente ou pausa).");
        }

        // 4. Double Booking Check (100% Seguro devido ao Lock)
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                input.professionalId(),
                input.startTime(),
                input.startTime().plusMinutes(totalDuration));

        if (hasConflict) {
            throw new ScheduleConflictException("Este horário acabou de ser ocupado por outro cliente.");
        }

        // 5. Criação do Objeto de Domínio
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
                input.reminderMinutes());

        // 6. Persistência (O registro é salvo e garantido no banco)
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento salvo no banco (Transação Ativa). ID: {}", savedAppointment.getId());

        // 7. Publicação do Evento (Assíncrono)
        // Isso coloca uma mensagem na memória do Spring. O Listener só vai pegar essa mensagem
        // DEPOIS que essa transação fizer o commit (AFTER_COMMIT), garantindo consistência.
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedAppointment.getId(),
                professional.getId(),
                client.getName(),
                savedAppointment.getStartTime()
        ));

        return savedAppointment;
    }

    // Input DTO (Record)
    public record CreateAppointmentInput(
            String clientId,
            String professionalId,
            List<String> serviceIds,
            LocalDateTime startTime,
            Integer reminderMinutes) {
    }
}