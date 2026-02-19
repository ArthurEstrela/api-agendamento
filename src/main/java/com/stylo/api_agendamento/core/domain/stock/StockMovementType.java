package com.stylo.api_agendamento.core.domain.stock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StockMovementType {
    
    // --- SAÍDAS (Diminuem estoque) ---
    SALE("Venda", StockDirection.OUTPUT),
    INTERNAL_USE("Uso Interno", StockDirection.OUTPUT),
    LOSS("Perda/Quebra", StockDirection.OUTPUT),
    RETURN_TO_SUPPLIER("Devolução ao Fornecedor", StockDirection.OUTPUT),

    // --- ENTRADAS (Aumentam estoque) ---
    RESTOCK("Compra/Reposição", StockDirection.INPUT),
    RETURN_FROM_CUSTOMER("Devolução de Cliente", StockDirection.INPUT),
    ADJUSTMENT_ENTRY("Ajuste de Entrada", StockDirection.INPUT);

    private final String description;
    private final StockDirection direction;

    public boolean isOutput() {
        return this.direction == StockDirection.OUTPUT;
    }
    
    public boolean isInput() {
        return this.direction == StockDirection.INPUT;
    }
    
    // Retorna 1 ou -1 para facilitar cálculos de soma
    public int getMultiplier() {
        return this.direction == StockDirection.INPUT ? 1 : -1;
    }

    public enum StockDirection {
        INPUT, OUTPUT
    }
}