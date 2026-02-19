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
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional // ✨ Garante consistência ao atualizar Agendamento + Retry
    public void processPendingSyncs() {
        List<GoogleSyncRetry> pendings = retryRepository.findByStatusAndNextRetryAtBefore(
                GoogleSyncRetry.SyncStatus.PENDING, LocalDateTime.now());

        if (pendings.isEmpty()) return;

        log.info("Processando {} sincronizações pendentes com Google Calendar...", pendings.size());

        for (GoogleSyncRetry retry : pendings) {
            try {
                processRetry(retry);
            } catch (Exception e) {
                log.warn("Falha na tentativa {} para o agendamento {}: {}", 
                        retry.getAttemptCount() + 1, retry.getAppointmentId(), e.getMessage());
                
                // ✨ CORREÇÃO: Usa o método de domínio para registrar a falha e calcular backoff
                retry.registerFailure(e.getMessage());
                retryRepository.save(retry);
            }
        }
    }

    private void processRetry(GoogleSyncRetry retry) {
        // Busca o agendamento no banco
        Appointment appt = appointmentRepository.findById(retry.getAppointmentId()).orElse(null);

        // Se o agendamento sumiu, não faz sentido continuar tentando
        if (appt == null) {
            log.warn("Agendamento {} não encontrado. Cancelando retry.", retry.getAppointmentId());
            retry.markAsCompleted(); // Ou criar um status ABORTED, mas COMPLETED resolve o loop
            retryRepository.save(retry);
            return;
        }

        // ✨ MELHORIA: Suporte a todas as operações (Create, Update, Delete)
        switch (retry.getOperation()) {
            case CREATE -> handleCreate(appt, retry);
            case UPDATE -> handleUpdate(appt, retry);
            case DELETE -> handleDelete(appt, retry);
        }
    }

    private void handleCreate(Appointment appt, GoogleSyncRetry retry) {
        // Se já tem ID externo, já foi sincronizado por outro fluxo
        if (appt.getExternalEventId() != null) {
            retry.markAsCompleted();
            retryRepository.save(retry);
            return;
        }

        String eventId = calendarProvider.createEvent(appt);
        if (eventId != null) {
            appt.setExternalEventId(eventId);
            appointmentRepository.save(appt);
            
            retry.markAsCompleted(); // ✨ CORREÇÃO: Usa método de domínio
            retryRepository.save(retry);
            log.info("Sincronização (CREATE) concluída para agendamento {}", appt.getId());
        }
    }

    private void handleUpdate(Appointment appt, GoogleSyncRetry retry) {
        if (appt.getExternalEventId() == null) {
            // Se não tem ID, não dá pra dar update. Tenta criar.
            handleCreate(appt, retry);
            return;
        }
        calendarProvider.updateEvent(appt);
        retry.markAsCompleted();
        retryRepository.save(retry);
    }

    private void handleDelete(Appointment appt, GoogleSyncRetry retry) {
        if (appt.getExternalEventId() != null) {
            calendarProvider.deleteEvent(appt.getExternalEventId(), appt.getProfessionalId());
        }
        retry.markAsCompleted();
        retryRepository.save(retry);
    }
}