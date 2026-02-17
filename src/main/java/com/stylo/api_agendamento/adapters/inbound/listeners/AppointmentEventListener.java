package com.stylo.api_agendamento.adapters.inbound.listeners;

import com.stylo.api_agendamento.adapters.outbound.persistence.google.GoogleSyncRetryRepository; // ✨ Import Adicionado
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.GoogleSyncRetry;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime; // ✨ Import Adicionado
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final IAppointmentRepository appointmentRepository;
    private final ICalendarProvider calendarProvider;
    private final INotificationProvider notificationProvider;
    private final IUserRepository userRepository;
    
    // ✨ INJEÇÃO QUE FALTAVA
    private final GoogleSyncRetryRepository retryRepository;

    /**
     * Listener Assíncrono.
     * Processa integrações que podem demorar (Google, Email, Push) sem travar o cliente.
     */
    @Async
    @EventListener
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Processando eventos pós-criação para Appointment: {}", event.appointmentId());

        try {
            // 1. Busca dados completos do agendamento
            Appointment appointment = appointmentRepository.findById(event.appointmentId())
                    .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

            // 2. Integração Google Calendar
            try {
                String googleEventId = calendarProvider.createEvent(appointment);
                
                if (googleEventId != null) {
                    appointment.setExternalEventId(googleEventId);
                    appointmentRepository.save(appointment);
                }
            } catch (Exception e) {
                log.error("Falha ao sincronizar Google. Agendando retry. Erro: {}", e.getMessage());
                // ✨ Agenda o Retry em caso de falha técnica
                scheduleRetry(event.appointmentId(), event.professionalId(), GoogleSyncRetry.SyncOperation.CREATE, e.getMessage());
            }

            // 3. Envia Notificações (Push/Email)
            // Executamos fora do try-catch do Google para garantir que a notificação saia
            // mesmo que o Google falhe.
            sendNotifications(appointment, event.professionalId());

        } catch (Exception e) {
            log.error("Erro fatal no Listener de Agendamento: {}", e.getMessage());
        }
    }

    private void sendNotifications(Appointment appt, String professionalId) {
        try {
            if (appt.getServices() == null || appt.getServices().isEmpty()) {
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
                    .ifPresent(u -> recipientIds.add(u.getId())); // Profissional específico

            for (String userId : recipientIds) {
                notificationProvider.sendNotification(userId, title, body, "/dashboard/agenda");
            }
        } catch (Exception e) {
            log.error("Falha ao enviar notificações (Background): {}", e.getMessage());
        }
    }

    private void scheduleRetry(String appointmentId, String profId, GoogleSyncRetry.SyncOperation op, String error) {
        // Verifica duplicidade usando o repositório injetado
        if (retryRepository.existsByAppointmentIdAndOperationAndStatus(appointmentId, op, GoogleSyncRetry.SyncStatus.PENDING)) {
            return;
        }

        GoogleSyncRetry retry = GoogleSyncRetry.builder()
                .appointmentId(appointmentId)
                .professionalId(profId)
                .operation(op)
                .attempts(0)
                .lastError(error)
                .status(GoogleSyncRetry.SyncStatus.PENDING)
                .nextRetryAt(LocalDateTime.now().plusMinutes(2)) // Tenta de novo em 2 min
                .build();
        
        retryRepository.save(retry);
    }
}