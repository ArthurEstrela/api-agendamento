package com.stylo.api_agendamento.adapters.inbound.listeners;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
// java.util.UUID removido pois não é mais necessário aqui

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final IAppointmentRepository appointmentRepository;
    private final ICalendarProvider calendarProvider;
    private final INotificationProvider notificationProvider;
    private final IUserRepository userRepository;

    /**
     * Listener Assíncrono.
     * phase = AFTER_COMMIT: Garante que o agendamento JÁ ESTÁ no banco antes de tentarmos ler.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Iniciando processamento assíncrono para agendamento: {}", event.appointmentId());

        // CORREÇÃO: Passamos o ID diretamente como String, conforme definido na Interface do Core.
        // O Adapter de persistência que lidará com a conversão para UUID se necessário.
        var appointmentOpt = appointmentRepository.findById(event.appointmentId());

        if (appointmentOpt.isEmpty()) {
            log.error("ERRO CRÍTICO: Agendamento {} não encontrado no listener após commit.", event.appointmentId());
            return;
        }

        Appointment appointment = appointmentOpt.get();

        // 2. Integração Google Calendar
        syncGoogleCalendar(appointment);

        // 3. Notificações
        sendNotifications(appointment, event.professionalId());
    }

    private void syncGoogleCalendar(Appointment appointment) {
        try {
            String googleEventId = calendarProvider.createEvent(appointment);
            if (googleEventId != null) {
                appointment.setExternalEventId(googleEventId);
                // Salvamos apenas a atualização do ID externo
                appointmentRepository.save(appointment); 
                log.info("Google Calendar sincronizado com sucesso via Listener.");
            }
        } catch (Exception e) {
            // Logamos o erro, mas NÃO quebramos o fluxo, pois o agendamento principal já está salvo.
            log.error("Falha ao sincronizar Google Calendar (Background): {}", e.getMessage());
        }
    }

    private void sendNotifications(Appointment appt, String professionalId) {
        try {
            // Verifica se a lista de serviços não está vazia para evitar IndexOutOfBounds
            if (appt.getServices() == null || appt.getServices().isEmpty()) {
                log.warn("Agendamento {} sem serviços para notificar.", appt.getId());
                return;
            }

            String mainServiceName = appt.getServices().get(0).getName();
            if (appt.getServices().size() > 1) mainServiceName += "...";

            String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"));
            String title = "📅 Novo Agendamento!";
            String body = String.format("%s agendou %s para %s", 
                appt.getClientName(), mainServiceName, dateFormatted);

            Set<String> recipientIds = new HashSet<>();
            recipientIds.add(appt.getServiceProviderId()); // Dono do estabelecimento
            
            userRepository.findByProfessionalId(professionalId)
                .ifPresent(u -> recipientIds.add(u.getId())); // Profissional (se tiver login)

            for (String userId : recipientIds) {
                notificationProvider.sendNotification(userId, title, body, "/dashboard/agenda");
            }
        } catch (Exception e) {
            log.error("Falha ao enviar notificações (Background): {}", e.getMessage());
        }
    }
}