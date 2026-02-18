package com.stylo.api_agendamento.adapters.outbound.persistence.coupon;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import com.stylo.api_agendamento.core.domain.coupon.DiscountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider_id", "code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponEntity extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "provider_id", nullable = false, length = 36)
    private String providerId;

    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType type;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "max_usages")
    private Integer maxUsages;

    @Column(name = "current_usages", nullable = false)
    @Builder.Default
    private Integer currentUsages = 0;

    @Column(name = "min_purchase_value")
    private BigDecimal minPurchaseValue;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}