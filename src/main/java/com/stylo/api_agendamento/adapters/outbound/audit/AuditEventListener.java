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

    @Async
    @EventListener
    public void handleAuditEvent(AuditLogEvent event) {
        try {
            // ✨ Em Java Records, acessamos os campos sem o prefixo "get"
            var logEntity = AuditLogEntity.builder()
                    .providerId(event.providerId()) // Adicionado campo faltante
                    .entityName(event.entityName())
                    .entityId(event.entityId())
                    .action(event.action())
                    .fieldName(event.fieldName())
                    .oldValue(event.oldValue())
                    .newValue(event.newValue())
                    .modifiedBy(event.modifiedBy())
                    .modifiedAt(event.timestamp())
                    .build();

            auditRepository.save(logEntity);
            log.info("Auditoria registrada: {} {} alterado por {}", 
                    event.entityName(), event.fieldName(), event.modifiedBy());

        } catch (Exception e) {
            log.error("Falha ao salvar log de auditoria", e);
        }
    }
}