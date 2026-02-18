package com.stylo.api_agendamento.core.domain.events;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogEvent {
    private final String entityName;
    private final String entityId;
    private final String action;
    private final String fieldName;
    private final String oldValue;
    private final String newValue;
    private final String modifiedBy;
    private final LocalDateTime timestamp;

    public static AuditLogEvent createUpdate(String entityName, String entityId, String field, String oldVal, String newVal, String userId) {
        return AuditLogEvent.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action("UPDATE")
                .fieldName(field)
                .oldValue(oldVal)
                .newValue(newVal)
                .modifiedBy(userId)
                .timestamp(LocalDateTime.now())
                .build();
    }
}