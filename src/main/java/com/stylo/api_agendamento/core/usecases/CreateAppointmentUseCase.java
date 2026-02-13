package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IUserRepository userRepository;
    private final ICalendarProvider calendarProvider;
    private final INotificationProvider notificationProvider;

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

        // 4. Double Booking Check
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                appointment.getProfessionalId(),
                appointment.getStartTime(),
                appointment.getEndTime());

        if (hasConflict) {
            throw new BusinessException("Este horário acabou de ser ocupado.");
        }

        // 5. Persistência Principal
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado com sucesso: ID {}", savedAppointment.getId());

        // 6. Integrações Externas
        syncWithGoogleCalendar(savedAppointment);
        triggerNotifications(savedAppointment, professional);

        return savedAppointment;
    }

    private void syncWithGoogleCalendar(Appointment appointment) {
        try {
            String googleEventId = calendarProvider.createEvent(appointment);
            if (googleEventId != null) {
                appointment.setExternalEventId(googleEventId);
                appointmentRepository.save(appointment);
                log.info("Google Calendar sincronizado.");
            }
        } catch (Exception e) {
            log.error("Erro na sincronização Google: {}", e.getMessage());
        }
    }

    private void triggerNotifications(Appointment appt, Professional prof) {
        try {
            // Ajuste aqui: Como pode ter vários serviços, pegamos o nome do primeiro para o texto
            String mainServiceName = appt.getServices().get(0).getName();
            if (appt.getServices().size() > 1) mainServiceName += "...";

            String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"));
            String title = "📅 Novo Agendamento!";
            String body = String.format("%s agendou %s para %s", 
                appt.getClientName(), mainServiceName, dateFormatted);

            Set<String> recipientIds = new HashSet<>();
            recipientIds.add(prof.getServiceProviderId());
            
            // Agora o método existe na interface
            userRepository.findByProfessionalId(prof.getId())
                .ifPresent(u -> recipientIds.add(u.getId()));

            for (String userId : recipientIds) {
                // Agora o método existe na interface
                notificationProvider.sendNotification(userId, title, body);
            }
            
            log.info("Notificações enviadas.");
        } catch (Exception e) {
            log.error("Erro ao disparar notificações: {}", e.getMessage());
        }
    }

    public record CreateAppointmentInput(
            String clientId,
            String professionalId,
            List<String> serviceIds,
            LocalDateTime startTime,
            Integer reminderMinutes) {
    }
}