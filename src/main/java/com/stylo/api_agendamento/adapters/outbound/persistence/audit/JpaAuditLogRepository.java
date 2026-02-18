package com.stylo.api_agendamento.adapters.outbound.persistence.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    // Futuramente você pode buscar: findAllByEntityNameAndEntityIdOrderByModifiedAtDesc
}