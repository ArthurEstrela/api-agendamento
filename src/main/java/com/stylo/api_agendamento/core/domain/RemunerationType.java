package com.stylo.api_agendamento.core.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum RemunerationType {
    PERCENTAGE {
        @Override
        public BigDecimal calculate(BigDecimal servicePrice, BigDecimal commissionValue) {
            if (servicePrice == null || commissionValue == null) return BigDecimal.ZERO;
            // Ex: Preço 100.00 * (40 / 100) = 40.00
            return servicePrice.multiply(commissionValue)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        }
    },
    FIXED_AMOUNT {
        @Override
        public BigDecimal calculate(BigDecimal servicePrice, BigDecimal commissionValue) {
            // Se for valor fixo, retorna o próprio valor (ex: R$ 20,00 por corte)
            // Mas nunca pode ser maior que o preço do serviço (segurança)
            if (commissionValue == null) return BigDecimal.ZERO;
            return commissionValue.min(servicePrice != null ? servicePrice : BigDecimal.ZERO);
        }
    };

    // Método abstrato que cada tipo implementa
    public abstract BigDecimal calculate(BigDecimal servicePrice, BigDecimal commissionValue);
}