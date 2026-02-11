package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
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
    private final ICalendarProvider calendarProvider;

    public Appointment execute(CreateAppointmentInput input) {
        // 1. Validações Iniciais
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        User client = userRepository.findById(input.clientId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));

        List<Service> requestedServices = serviceRepository.findAllByIds(input.serviceIds());
        if (requestedServices.isEmpty()) {
            throw new BusinessException("Selecione ao menos um serviço.");
        }

        // 2. Regras de Negócio de Disponibilidade
        professional.validateCanPerform(requestedServices);

        int totalDuration = requestedServices.stream().mapToInt(Service::getDuration).sum();
        if (!professional.isAvailable(input.startTime(), totalDuration)) {
            throw new BusinessException("Profissional indisponível neste horário.");
        }

        // 3. Criação do Objeto de Domínio
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

        // 4. Double Booking Check (Concorrência)
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                appointment.getProfessionalId(),
                appointment.getStartTime(),
                appointment.getEndTime());

        if (hasConflict) {
            throw new BusinessException("Este horário acabou de ser ocupado.");
        }

        // 5. Primeira Persistência (Garante o ID do agendamento)
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // 6. Integração Google Calendar (Pós-persistência)
        try {
            // Agora o método retorna String corretamente
            String googleEventId = calendarProvider.createEvent(savedAppointment);

            if (googleEventId != null) {
                // Atualiza o objeto com o ID externo
                savedAppointment.setExternalEventId(googleEventId);

                // Salva novamente para persistir o vínculo
                appointmentRepository.save(savedAppointment);

                log.info("Sincronização Google OK. EventID: {}", googleEventId);
            }
        } catch (Exception e) {
            // Falha silenciosa para não cancelar o agendamento no sistema
            log.error("Erro na sincronização com Google Calendar: {}", e.getMessage());
        }

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