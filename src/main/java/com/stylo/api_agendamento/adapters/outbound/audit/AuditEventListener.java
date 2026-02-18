package com.stylo.api_agendamento.adapters.outbound.audit;

import com.stylo.api_agendamento.adapters.outbound.persistence.audit.AuditLogEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.audit.JpaAuditLogRepository;
import com.stylo.api_agendamento.core.domain.events.AuditLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final JpaAuditLogRepository auditRepository;

    @Async // ✨ Importante: Executa em outra thread para não impactar performance
    @EventListener
    public void handleAuditEvent(AuditLogEvent event) {
        try {
            var logEntity = AuditLogEntity.builder()
                    .entityName(event.getEntityName())
                    .entityId(event.getEntityId())
                    .action(event.getAction())
                    .fieldName(event.getFieldName())
                    .oldValue(event.getOldValue())
                    .newValue(event.getNewValue())
                    .modifiedBy(event.getModifiedBy())
                    .modifiedAt(event.getTimestamp())
                    .build();

            auditRepository.save(logEntity);
            log.info("Auditoria registrada: {} {} alterado por {}", event.getEntityName(), event.getFieldName(), event.getModifiedBy());

        } catch (Exception e) {
            // Auditoria não deve quebrar o sistema, mas deve ser logada se falhar
            log.error("Falha ao salvar log de auditoria", e);
        }
    }
}