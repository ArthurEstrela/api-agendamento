package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import com.stylo.api_agendamento.core.domain.stock.StockMovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class StockMovementEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // ✨ Corrigido para UUID

    @Column(name = "product_id", nullable = false)
    private UUID productId; // ✨ Corrigido para UUID

    @Column(name = "service_provider_id", nullable = false)
    private UUID serviceProviderId; // Verifique se não está apenas 'providerId'

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "performed_by_user_id")
    private UUID performedByUserId; // ✨ Corrigido para UUID

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}