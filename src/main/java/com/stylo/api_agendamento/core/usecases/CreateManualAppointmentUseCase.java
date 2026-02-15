package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException;
import com.stylo.api_agendamento.core.ports.*;
import jakarta.transaction.Transactional; // Importante para integridade
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateManualAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    // ✨ Necessário para buscar o TimeZone
    private final IServiceProviderRepository serviceProviderRepository; 
    // ✨ Necessário para sincronizar com Google Calendar
    private final IEventPublisher eventPublisher; 

    @Transactional
    public Appointment execute(ManualInput input) {
        // 1. Busca o Profissional
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Busca e valida os serviços
        List<Service> services = serviceRepository.findAllByIds(input.serviceIds());
        if (services.isEmpty()) throw new BusinessException("Selecione ao menos um serviço.");

        // 3. Validação de conflitos e grade
        int duration = services.stream().mapToInt(Service::getDuration).sum();
        if (!professional.isAvailable(input.startTime(), duration)) {
            throw new BusinessException("Horário fora da grade do profissional.");
        }

        // 4. Recuperação do TimeZone (Igual ao agendamento online)
        String timeZone = serviceProviderRepository.findById(professional.getServiceProviderId())
                .map(ServiceProvider::getTimeZone)
                .orElse("America/Sao_Paulo");

        // 5. Criação usando a nova fábrica de Walk-in (Passando TimeZone)
        Appointment appointment = Appointment.createManual(
                input.clientName(),
                new ClientPhone(input.clientPhone()),
                professional.getServiceProviderId(),
                professional.getId(),
                professional.getName(),
                services,
                input.startTime(),
                input.notes(),
                timeZone // ✨ Argumento corrigido
        );

        // 6. Proteção de Conflito Atômica
        if (appointmentRepository.hasConflictingAppointment(
                appointment.getProfessionalId(), appointment.getStartTime(), appointment.getEndTime())) {
            throw new ScheduleConflictException("Conflito de horário detectado.");
        }

        // 7. Persistência
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento manual criado: {}", savedAppointment.getId());

        // 8. Disparo de Evento (Para ir pro Google Calendar)
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedAppointment.getId(),
                professional.getId(),
                input.clientName(), // Nome do cliente manual
                savedAppointment.getStartTime()
        ));

        return savedAppointment;
    }

    public record ManualInput(
            String professionalId,
            String clientName,
            String clientPhone,
            List<String> serviceIds,
            LocalDateTime startTime,
            String notes
    ) {}
}