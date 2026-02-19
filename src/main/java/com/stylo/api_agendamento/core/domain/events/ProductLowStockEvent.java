package com.stylo.api_agendamento.core.domain.events;

import java.util.UUID;

public record ProductLowStockEvent(
    UUID productId,
    UUID providerId,
    String productName,
    Integer currentStock,
    Integer minThreshold
) {}