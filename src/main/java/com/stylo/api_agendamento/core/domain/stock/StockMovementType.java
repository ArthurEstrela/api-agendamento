package com.stylo.api_agendamento.core.domain.stock;

public enum StockMovementType {
    SALE,           // Venda para cliente (via Agendamento/POS)
    INTERNAL_USE,   // Consumo interno (Profissional usou)
    RESTOCK,        // Compra de fornecedor (Entrada)
    ADJUSTMENT,     // Correção de inventário (Perda/Quebra)
    RETURN          // Devolução de cliente
}