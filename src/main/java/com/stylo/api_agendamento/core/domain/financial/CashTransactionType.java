package com.stylo.api_agendamento.core.domain.financial;

public enum CashTransactionType {
    OPENING,        // Abertura de caixa (saldo inicial)
    SALE,           // Venda em dinheiro/pix físico registrada no caixa
    BLEED,          // Sangria (retirada de dinheiro para pagar algo ou depositar)
    REINFORCEMENT,  // Suprimento (adicionar troco)
    CLOSING         // Fechamento (conferência final)
}