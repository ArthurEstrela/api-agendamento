package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.stock.StockMovement;

public interface IStockMovementRepository {
    StockMovement save(StockMovement movement);
    // Futuramente: List<StockMovement> findByProductId(String productId);
}