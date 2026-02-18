package com.stylo.api_agendamento.adapters.outbound.persistence.coupon;

import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public Coupon toDomain(CouponEntity entity) {
        if (entity == null) return null;

        return Coupon.builder()
                .id(entity.getId())
                .providerId(entity.getProviderId())
                .code(entity.getCode())
                .type(entity.getType())
                .value(entity.getValue())
                .expirationDate(entity.getExpirationDate())
                .maxUsages(entity.getMaxUsages())
                .currentUsages(entity.getCurrentUsages())
                .minPurchaseValue(entity.getMinPurchaseValue())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CouponEntity toEntity(Coupon domain) {
        if (domain == null) return null;

        return CouponEntity.builder()
                .id(domain.getId())
                .providerId(domain.getProviderId())
                .code(domain.getCode())
                .type(domain.getType())
                .value(domain.getValue())
                .expirationDate(domain.getExpirationDate())
                .maxUsages(domain.getMaxUsages())
                .currentUsages(domain.getCurrentUsages() != null ? domain.getCurrentUsages() : 0)
                .minPurchaseValue(domain.getMinPurchaseValue())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}