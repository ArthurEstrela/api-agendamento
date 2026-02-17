package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
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
@UseCase
@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IUserRepository userRepository;
    
    // Repositório para buscar configurações do estabelecimento (TimeZone)
    private final IServiceProviderRepository serviceProviderRepository;

    // Publicador de eventos para integrações assíncronas (Google, Notificações)
    private final IEventPublisher eventPublisher;

    @Transactional
    public Appointment execute(CreateAppointmentInput input) {
        
        // 1. Busca Profissional COM LOCK (Pessimistic Locking para evitar race conditions)
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

        // 4. Validação de Competência e Horário (Regras de Domínio)
        professional.validateCanPerform(requestedServices);

        int totalDuration = requestedServices.stream().mapToInt(Service::getDuration).sum();
        
        if (!professional.isAvailable(input.startTime(), totalDuration)) {
            throw new BusinessException("Profissional indisponível neste horário (fora do expediente ou pausa).");
        }

        // 5. Proteção contra Double Booking (Verificação Final)
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                input.professionalId(),
                input.startTime(),
                input.startTime().plusMinutes(totalDuration));

        if (hasConflict) {
            throw new ScheduleConflictException("Este horário acabou de ser ocupado por outro cliente.");
        }

        // 6. Recuperação do TimeZone do Estabelecimento
        // Isso garante que o agendamento seja salvo com o contexto de fuso horário correto
        String timeZone = serviceProviderRepository.findById(professional.getServiceProviderId())
                .map(ServiceProvider::getTimeZone)
                .orElse("America/Sao_Paulo"); // Fallback seguro

        // 7. Criação do Objeto de Domínio (A Entidade valida preços negativos e datas)
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
                timeZone // ✨ TimeZone Injetado
        );

        // 8. Persistência
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado (ID: {}). TimeZone: {}", savedAppointment.getId(), timeZone);

        // 9. Publicação do Evento Assíncrono (Google Calendar será chamado pelo Listener)
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedAppointment.getId(),
                professional.getId(),
                client.getName(),
                savedAppointment.getStartTime()
        ));

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