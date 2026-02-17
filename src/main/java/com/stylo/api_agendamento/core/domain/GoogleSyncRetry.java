package com.stylo.api_agendamento.core.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "google_sync_retries")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class GoogleSyncRetry {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String appointmentId;
    private String professionalId;

    @Enumerated(EnumType.STRING)
    private SyncOperation operation; // CREATE, UPDATE, DELETE

    private int attempts;
    private String lastError;
    private LocalDateTime nextRetryAt;

    @Enumerated(EnumType.STRING)
    private SyncStatus status; // PENDING, FAILED, COMPLETED

    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    public enum SyncOperation { CREATE, UPDATE, DELETE }
    public enum SyncStatus { PENDING, FAILED, COMPLETED }
}