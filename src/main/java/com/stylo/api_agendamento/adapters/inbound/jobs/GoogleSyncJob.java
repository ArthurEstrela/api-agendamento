package com.stylo.api_agendamento.adapters.inbound.jobs;

import com.stylo.api_agendamento.adapters.outbound.persistence.google.GoogleSyncRetryRepository;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.GoogleSyncRetry;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class GoogleSyncJob {

    private final GoogleSyncRetryRepository retryRepository;
    private final IAppointmentRepository appointmentRepository;
    private final ICalendarProvider calendarProvider;

    @Scheduled(cron = "0 */5 * * * *") // Roda a cada 5 minutos
    public void processPendingSyncs() {
        List<GoogleSyncRetry> pendings = retryRepository.findByStatusAndNextRetryAtBefore(
                GoogleSyncRetry.SyncStatus.PENDING, LocalDateTime.now());

        if (!pendings.isEmpty()) {
            log.info("Encontrados {} agendamentos pendentes de sincronização Google.", pendings.size());
        }

        for (GoogleSyncRetry retry : pendings) {
            try {
                processRetry(retry);
            } catch (Exception e) {
                handleFailure(retry, e);
            }
        }
    }

    private void processRetry(GoogleSyncRetry retry) {
        if (retry.getOperation() == GoogleSyncRetry.SyncOperation.CREATE) {
            Appointment appt = appointmentRepository.findById(retry.getAppointmentId()).orElse(null);
            
            // Se o agendamento não existe mais ou foi cancelado, aborta
            if (appt == null || appt.getExternalEventId() != null) {
                retry.setStatus(GoogleSyncRetry.SyncStatus.COMPLETED);
                retryRepository.save(retry);
                return;
            }

            String eventId = calendarProvider.createEvent(appt);
            
            if (eventId != null) {
                appt.setExternalEventId(eventId);
                appointmentRepository.save(appt);
                
                retry.setStatus(GoogleSyncRetry.SyncStatus.COMPLETED);
                retryRepository.save(retry);
                log.info("Retry com sucesso para agendamento {}", appt.getId());
            } else {
                throw new RuntimeException("Google retornou null no retry");
            }
        }
    }

    private void handleFailure(GoogleSyncRetry retry, Exception e) {
        retry.setAttempts(retry.getAttempts() + 1);
        retry.setLastError(e.getMessage());

        if (retry.getAttempts() >= 5) { // Desiste após 5 tentativas
            retry.setStatus(GoogleSyncRetry.SyncStatus.FAILED);
            log.error("Retry falhou permanentemente para {}. Desistindo.", retry.getAppointmentId());
        } else {
            // Exponential Backoff: 2m, 4m, 8m, 16m...
            long delayMinutes = (long) Math.pow(2, retry.getAttempts());
            retry.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
        }
        retryRepository.save(retry);
    }
}