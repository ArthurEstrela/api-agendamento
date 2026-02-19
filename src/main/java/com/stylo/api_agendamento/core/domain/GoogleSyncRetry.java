package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GoogleSyncRetry {

    private UUID id;

    private UUID appointmentId;
    private UUID professionalId;

    private SyncOperation operation; // CREATE, UPDATE, DELETE

    private int attemptCount;
    private String lastError;
    private LocalDateTime nextRetryAt;

    private SyncStatus status; // PENDING, FAILED, COMPLETED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- CONSTANTES DE NEGÓCIO ---
    private static final int MAX_ATTEMPTS = 10;
    private static final int BASE_BACKOFF_MINUTES = 1;

    // --- FACTORY ---

    public static GoogleSyncRetry create(UUID appointmentId, UUID professionalId, SyncOperation operation) {
        if (appointmentId == null) throw new BusinessException("ID do agendamento é obrigatório para sincronização.");
        if (professionalId == null) throw new BusinessException("ID do profissional é obrigatório para sincronização.");
        if (operation == null) throw new BusinessException("Operação de sincronização inválida.");

        return GoogleSyncRetry.builder()
                .id(UUID.randomUUID())
                .appointmentId(appointmentId)
                .professionalId(professionalId)
                .operation(operation)
                .attemptCount(0)
                .status(SyncStatus.PENDING)
                .nextRetryAt(LocalDateTime.now()) // Primeira tentativa é imediata
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    /**
     * Registra uma falha na tentativa de sincronização e calcula o próximo horário (Backoff Exponencial).
     */
    public void registerFailure(String errorMessage) {
        this.attemptCount++;
        this.lastError = errorMessage != null && errorMessage.length() > 500 
                ? errorMessage.substring(0, 500) // Trunca erro muito longo
                : errorMessage;
        this.updatedAt = LocalDateTime.now();

        if (this.attemptCount >= MAX_ATTEMPTS) {
            this.status = SyncStatus.FAILED;
            this.nextRetryAt = null; // Sem novas tentativas
        } else {
            this.status = SyncStatus.PENDING;
            // Backoff exponencial: 1, 2, 4, 8, 16 minutos...
            long minutesToAdd = (long) (BASE_BACKOFF_MINUTES * Math.pow(2, this.attemptCount - 1));
            this.nextRetryAt = LocalDateTime.now().plusMinutes(minutesToAdd);
        }
    }

    public void markAsCompleted() {
        this.status = SyncStatus.COMPLETED;
        this.nextRetryAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRetryable() {
        return this.status == SyncStatus.PENDING && 
               this.attemptCount < MAX_ATTEMPTS &&
               (this.nextRetryAt != null && this.nextRetryAt.isBefore(LocalDateTime.now()));
    }
    
    // Reinicia o processo manualmente (ex: via painel administrativo)
    public void reset() {
        this.status = SyncStatus.PENDING;
        this.attemptCount = 0;
        this.lastError = null;
        this.nextRetryAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoogleSyncRetry that = (GoogleSyncRetry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- ENUMS ---

    public enum SyncOperation {
        CREATE,
        UPDATE,
        DELETE
    }

    public enum SyncStatus {
        PENDING,
        FAILED,
        COMPLETED
    }
}