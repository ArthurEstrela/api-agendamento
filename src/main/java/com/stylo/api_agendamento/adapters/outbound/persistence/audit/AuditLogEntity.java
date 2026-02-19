package com.stylo.api_agendamento.adapters.outbound.persistence.audit;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ✨ ISOLAMENTO SAAS: Garante que os logs sejam filtrados por salão
    @Column(name = "provider_id")
    private UUID providerId;

    @Column(nullable = false, name = "entity_name")
    private String entityName;

    // ✨ CORREÇÃO: Atualizado de String para UUID
    @Column(nullable = false, name = "entity_id")
    private UUID entityId;

    @Column(nullable = false)
    private String action; // Ex: UPDATE_COMMISSION, DELETE_SERVICE

    @Column(name = "field_name")
    private String fieldName;

    @Column(columnDefinition = "TEXT", name = "old_value")
    private String oldValue;

    @Column(columnDefinition = "TEXT", name = "new_value")
    private String newValue;

    // ✨ CORREÇÃO: Atualizado de String para UUID (Quem fez a alteração)
    @Column(name = "modified_by")
    private UUID modifiedBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @PrePersist
    protected void onCreate() {
        if (this.modifiedAt == null) {
            this.modifiedAt = LocalDateTime.now();
        }
    }
}