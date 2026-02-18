package com.stylo.api_agendamento.core.domain.stock;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class StockMovement {
    private String id;
    private String productId;
    private String providerId;
    private StockMovementType type;
    private Integer quantity; // Positivo para entrada, negativo para saída
    private String reason;    // Ex: "Usado no cabelo da Dona Maria"
    private String performedByUserId;
    private LocalDateTime createdAt;
}