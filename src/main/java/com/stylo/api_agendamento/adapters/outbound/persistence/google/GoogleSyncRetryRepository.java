package com.stylo.api_agendamento.adapters.outbound.persistence.google;

import com.stylo.api_agendamento.core.domain.GoogleSyncRetry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GoogleSyncRetryRepository extends JpaRepository<GoogleSyncRetry, UUID> {

    // Busca retries pendentes cujo horário de tentar já chegou
    List<GoogleSyncRetry> findByStatusAndNextRetryAtBefore(
            GoogleSyncRetry.SyncStatus status,
            LocalDateTime now);

    // Evita duplicar o retry para o mesmo agendamento/operação
    boolean existsByAppointmentIdAndOperationAndStatus(
            UUID appointmentId,
            GoogleSyncRetry.SyncOperation operation,
            GoogleSyncRetry.SyncStatus status);
}