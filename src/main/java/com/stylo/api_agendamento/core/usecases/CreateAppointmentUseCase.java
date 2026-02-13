package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException; // Recomendo criar essa exception específica
import com.stylo.api_agendamento.core.ports.*;
import jakarta.transaction.Transactional; // ⚠️ Importante: Do pacote jakarta.transaction
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

        // 4. Double Booking Check (Agora 100% Seguro devido ao Lock)
        // Como o profissional está travado, ninguém mais pode estar inserindo um agendamento
        // para ele neste exato momento. A leitura abaixo é garantida.
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

        // 6. Persistência (Commit acontece após o return)
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado com sucesso e horário blindado: ID {}", savedAppointment.getId());

        // 7. Integrações (Pós-persistência crítica)
        // Nota: Se o Google Calendar falhar, o agendamento no banco NÃO é desfeito
        // (idealmente, isso deveria ser assíncrono, mas síncrono funciona bem para MVP)
        performExternalIntegrations(savedAppointment, professional);

        return savedAppointment;
    }

    private void performExternalIntegrations(Appointment appointment, Professional professional) {
        // A. Sincronização Google Calendar
        try {
            String googleEventId = calendarProvider.createEvent(appointment);
            if (googleEventId != null) {
                appointment.setExternalEventId(googleEventId);
                appointmentRepository.save(appointment); // Atualiza com o ID externo
                log.info("Google Calendar sincronizado.");
            }
        } catch (Exception e) {
            log.error("Erro não-bloqueante na sincronização Google: {}", e.getMessage());
        }

        // B. Notificações
        triggerNotifications(appointment, professional);
    }

    private void triggerNotifications(Appointment appt, Professional prof) {
        try {
            String mainServiceName = appt.getServices().get(0).getName();
            if (appt.getServices().size() > 1) mainServiceName += "...";

            String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"));
            String title = "📅 Novo Agendamento!";
            String body = String.format("%s agendou %s para %s", 
                appt.getClientName(), mainServiceName, dateFormatted);

            Set<String> recipientIds = new HashSet<>();
            recipientIds.add(prof.getServiceProviderId()); // Dono
            
            userRepository.findByProfessionalId(prof.getId())
                .ifPresent(u -> recipientIds.add(u.getId())); // Profissional (se tiver usuário)

            for (String userId : recipientIds) {
                notificationProvider.sendNotification(userId, title, body, "/dashboard/agenda");
            }
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