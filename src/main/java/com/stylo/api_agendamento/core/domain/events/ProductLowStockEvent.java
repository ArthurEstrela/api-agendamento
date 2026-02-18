package com.stylo.api_agendamento.core.domain.events;

public record ProductLowStockEvent(
    String productId,
    String providerId,
    String productName,
    Integer currentStock,
    Integer minThreshold
) {}