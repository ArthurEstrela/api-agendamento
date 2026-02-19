package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateManualAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IServiceProviderRepository serviceProviderRepository; 
    private final IEventPublisher eventPublisher; 

    @Transactional
    public Appointment execute(Input input) {
        // 1. Busca Profissional e Estabelecimento
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        ServiceProvider provider = serviceProviderRepository.findById(professional.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 2. Busca e valida os serviços selecionados
        List<Service> services = serviceRepository.findAllByIds(input.serviceIds());
        if (services.isEmpty()) {
            throw new BusinessException("Selecione ao menos um serviço para o agendamento.");
        }

        // 3. Validação de Disponibilidade na Grade (Regra de Domínio)
        int totalDuration = services.stream().mapToInt(Service::getDuration).sum();
        if (!professional.isAvailable(input.startTime(), totalDuration)) {
            throw new BusinessException("O horário solicitado está fora do expediente ou configurado como pausa.");
        }

        // 4. Criação da Entidade via Factory de Domínio (Agendamento Manual/Walk-in)
        Appointment appointment = Appointment.createManual(
                input.clientName(),
                new ClientPhone(input.clientPhone()),
                provider.getId(),
                professional.getId(),
                professional.getName(),
                services,
                input.startTime(),
                input.notes(),
                provider.getTimeZone() // Garante persistência com fuso horário correto
        );

        // 5. Check de Conflito Atômico (Double Booking)
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                appointment.getProfessionalId(), 
                appointment.getStartTime(), 
                appointment.getEndTime()
        );

        if (hasConflict) {
            throw new ScheduleConflictException("Este horário já foi ocupado. Verifique a agenda.");
        }

        // 6. Persistência
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento manual (Walk-in) criado com sucesso: ID {}", savedAppointment.getId());

        // 7. Sincronização Externa (Gatilho para Google Calendar)
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedAppointment.getId(),
                professional.getId(),
                input.clientName(), 
                savedAppointment.getStartTime()
        ));

        return savedAppointment;
    }

    /**
     * Input do Agendamento Manual com UUIDs
     */
    public record Input(
            UUID professionalId,
            String clientName,
            String clientPhone,
            List<UUID> serviceIds,
            LocalDateTime startTime,
            String notes
    ) {}
}