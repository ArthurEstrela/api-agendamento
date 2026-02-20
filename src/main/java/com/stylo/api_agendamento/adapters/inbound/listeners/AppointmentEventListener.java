package com.stylo.api_agendamento.adapters.inbound.listeners;

import com.stylo.api_agendamento.adapters.outbound.persistence.google.GoogleSyncRetryRepository;
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

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final IAppointmentRepository appointmentRepository;
    private final ICalendarProvider calendarProvider;
    private final INotificationProvider notificationProvider;
    private final IUserRepository userRepository;
    private final GoogleSyncRetryRepository retryRepository;

    @Async
    @EventListener
    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("Processando eventos pós-criação para Appointment: {}", event.appointmentId());

        try {
            Appointment appointment = appointmentRepository.findById(event.appointmentId())
                    .orElseThrow(() -> new RuntimeException("Agendamento não encontrado"));

            // 1. Sincronização Google Calendar
            try {
                String googleEventId = calendarProvider.createEvent(appointment);
                if (googleEventId != null) {
                    appointment.setExternalEventId(googleEventId);
                    appointmentRepository.save(appointment);
                }
            } catch (Exception e) {
                log.error("Falha ao sincronizar Google. Agendando retry: {}", e.getMessage());
                scheduleRetry(appointment.getId(), appointment.getProfessionalId(), 
                              GoogleSyncRetry.SyncOperation.CREATE, e.getMessage());
            }

            // 2. Disparo de Notificações
            sendNotifications(appointment);

        } catch (Exception e) {
            log.error("Erro fatal no Listener de Agendamento", e);
        }
    }

    private void sendNotifications(Appointment appt) {
        try {
            String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"));
            String title = "📅 Novo Agendamento!";
            String body = String.format("%s agendou %s para %s",
                    appt.getClientName(), appt.getServicesSnapshot(), dateFormatted);

            // Conjunto para evitar enviar duas vezes para a mesma pessoa
            Set<UUID> userIdsToNotify = new HashSet<>();
            
            // Adiciona o Profissional que vai atender
            userRepository.findByProfessionalId(appt.getProfessionalId())
                    .ifPresent(u -> userIdsToNotify.add(u.getId()));

            // Adiciona o Dono do Estabelecimento (Provider)
            userRepository.findByProviderId(appt.getServiceProviderId())
                    .ifPresent(u -> userIdsToNotify.add(u.getId()));

            // Envia para todos os interessados
            for (UUID userId : userIdsToNotify) {
                notificationProvider.sendPushNotification(userId, title, body, "/dashboard/agenda");
            }

        } catch (Exception e) {
            log.error("Falha ao enviar notificações: {}", e.getMessage());
        }
    }

    private void scheduleRetry(UUID appointmentId, UUID professionalId, GoogleSyncRetry.SyncOperation op, String error) {
        // ✨ CORREÇÃO: Agora o repositório aceita UUID corretamente
        if (retryRepository.existsByAppointmentIdAndOperationAndStatus(appointmentId, op, GoogleSyncRetry.SyncStatus.PENDING)) {
            return;
        }

        // ✨ CORREÇÃO: Usando o Factory Method do domínio para garantir as regras de backoff
        GoogleSyncRetry retry = GoogleSyncRetry.create(appointmentId, professionalId, op);
        retry.registerFailure(error); // Calcula o backoff automático para a próxima tentativa
        
        retryRepository.save(retry);
    }
}