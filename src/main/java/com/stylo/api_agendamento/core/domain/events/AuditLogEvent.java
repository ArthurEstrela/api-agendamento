package com.stylo.api_agendamento.core.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogEvent(
    UUID eventId,
    String entityName,
    UUID entityId,
    UUID providerId, // ✨ Contexto do Estabelecimento (Tenant)
    String action,   // CREATE, UPDATE, DELETE
    String fieldName,
    String oldValue,
    String newValue,
    UUID modifiedBy,
    LocalDateTime timestamp
) {
    // --- FACTORY METHODS PARA FACILITAR ---

    public static AuditLogEvent createUpdate(String entityName, UUID entityId, UUID providerId, 
                                             String field, String oldVal, String newVal, UUID userId) {
        return new AuditLogEvent(
                UUID.randomUUID(),
                entityName,
                entityId,
                providerId,
                "UPDATE",
                field,
                oldVal,
                newVal,
                userId,
                LocalDateTime.now()
        );
    }

    public static AuditLogEvent createInsert(String entityName, UUID entityId, UUID providerId, UUID userId) {
        return new AuditLogEvent(
                UUID.randomUUID(),
                entityName,
                entityId,
                providerId,
                "CREATE",
                null, null, null,
                userId,
                LocalDateTime.now()
        );
    }
    
    public static AuditLogEvent createDelete(String entityName, UUID entityId, UUID providerId, UUID userId) {
        return new AuditLogEvent(
                UUID.randomUUID(),
                entityName,
                entityId,
                providerId,
                "DELETE",
                null, null, null,
                userId,
                LocalDateTime.now()
        );
    }
}