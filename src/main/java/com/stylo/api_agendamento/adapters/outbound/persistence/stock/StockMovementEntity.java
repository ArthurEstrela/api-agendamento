package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import com.stylo.api_agendamento.core.domain.stock.StockMovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementEntity extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "provider_id", nullable = false, length = 36)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    @Column(nullable = false)
    private Integer quantity;

    private String reason;

    @Column(name = "performed_by_user_id", length = 36)
    private String performedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}