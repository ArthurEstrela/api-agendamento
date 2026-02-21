package com.stylo.api_agendamento.adapters.outbound.persistence.google;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GoogleSyncRetryRepository extends JpaRepository<GoogleSyncRetryEntity, UUID> {

    // Status agora é String para bater com a entidade
    List<GoogleSyncRetryEntity> findByStatusAndNextRetryAtBefore(
            String status,
            LocalDateTime now);

    boolean existsByAppointmentIdAndOperationAndStatus(
            UUID appointmentId,
            String operation,
            String status);
}