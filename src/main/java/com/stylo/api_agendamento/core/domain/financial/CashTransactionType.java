package com.stylo.api_agendamento.core.domain.financial;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum CashTransactionType {

    OPENING("Abertura de Caixa", TransactionOperation.ADD),
    
    SALE("Venda (Dinheiro)", TransactionOperation.ADD),
    
    BLEED("Sangria", TransactionOperation.SUBTRACT),
    
    REINFORCEMENT("Suprimento/Reforço", TransactionOperation.ADD),
    
    CLOSING("Fechamento", TransactionOperation.NEUTRAL); // Apenas marca o fim, não altera saldo calculado

    private final String description;
    private final TransactionOperation operation;

    public BigDecimal apply(BigDecimal currentBalance, BigDecimal amount) {
        if (this.operation == TransactionOperation.ADD) {
            return currentBalance.add(amount);
        } else if (this.operation == TransactionOperation.SUBTRACT) {
            return currentBalance.subtract(amount);
        }
        return currentBalance;
    }

    public boolean isSubtract() {
        return this.operation == TransactionOperation.SUBTRACT;
    }

    private enum TransactionOperation {
        ADD, SUBTRACT, NEUTRAL
    }
}