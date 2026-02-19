package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.stock.StockMovement;

import java.util.UUID;

public interface IStockMovementRepository {
    
    StockMovement save(StockMovement movement);

    /**
     * Histórico de movimentações de um produto específico (Kardex).
     */
    PagedResult<StockMovement> findAllByProductId(UUID productId, int page, int size);

    /**
     * Auditoria geral do estabelecimento (quem mexeu no que).
     */
    PagedResult<StockMovement> findAllByProviderId(UUID providerId, int page, int size);
}