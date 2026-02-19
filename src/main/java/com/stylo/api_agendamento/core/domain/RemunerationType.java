package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@RequiredArgsConstructor
public enum RemunerationType {

    PERCENTAGE("Porcentagem") {
        @Override
        public BigDecimal calculate(BigDecimal servicePrice, BigDecimal commissionValue) {
            if (servicePrice == null || commissionValue == null) return BigDecimal.ZERO;
            
            // Ex: Preço 100.00 * 40 / 100 = 40.00
            return servicePrice.multiply(commissionValue)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
        }

        @Override
        public void validateValue(BigDecimal value) {
            if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException("A porcentagem de comissão deve ser entre 0 e 100.");
            }
        }
    },

    FIXED_AMOUNT("Valor Fixo") {
        @Override
        public BigDecimal calculate(BigDecimal servicePrice, BigDecimal commissionValue) {
            if (commissionValue == null) return BigDecimal.ZERO;
            BigDecimal price = servicePrice != null ? servicePrice : BigDecimal.ZERO;
            
            // Segurança: A comissão fixa não pode exceder o valor do serviço (evita saldo negativo)
            return commissionValue.min(price);
        }

        @Override
        public void validateValue(BigDecimal value) {
            if (value.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("O valor fixo da comissão não pode ser negativo.");
            }
        }
    };

    private final String description;

    public abstract BigDecimal calculate(BigDecimal servicePrice, BigDecimal commissionValue);
    
    public abstract void validateValue(BigDecimal value);
}