package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import org.springframework.stereotype.Component;

@Component
public class StockMovementMapper {

    public StockMovement toDomain(StockMovementEntity entity) {
        if (entity == null) return null;

        return StockMovement.builder()
                .id(entity.getId())
                .productId(entity.getProductId())
                .providerId(entity.getProviderId())
                .type(entity.getType())
                .quantity(entity.getQuantity())
                .reason(entity.getReason())
                .performedByUserId(entity.getPerformedByUserId())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public StockMovementEntity toEntity(StockMovement domain) {
        if (domain == null) return null;

        return StockMovementEntity.builder()
                .id(domain.getId())
                .productId(domain.getProductId())
                .providerId(domain.getProviderId())
                .type(domain.getType())
                .quantity(domain.getQuantity())
                .reason(domain.getReason())
                .performedByUserId(domain.getPerformedByUserId())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}