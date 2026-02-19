package com.stylo.api_agendamento.core.domain.coupon;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@RequiredArgsConstructor
public enum DiscountType {

    PERCENTAGE("Porcentagem") {
        @Override
        public BigDecimal calculate(BigDecimal originalPrice, BigDecimal discountValue) {
            if (originalPrice == null || discountValue == null) return BigDecimal.ZERO;
            
            // Regra: Valor * (Porcentagem / 100)
            BigDecimal discountAmount = originalPrice.multiply(discountValue)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
            
            // Segurança: O desconto não pode ser maior que o preço
            return discountAmount.min(originalPrice);
        }

        @Override
        public void validateValue(BigDecimal value) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(new BigDecimal("100")) > 0) {
                throw new BusinessException("A porcentagem de desconto deve ser maior que 0 e menor ou igual a 100.");
            }
        }
    },

    FIXED("Valor Fixo") {
        @Override
        public BigDecimal calculate(BigDecimal originalPrice, BigDecimal discountValue) {
            if (originalPrice == null || discountValue == null) return BigDecimal.ZERO;
            
            // Segurança: O desconto fixo (ex: R$ 50) não pode deixar o preço negativo
            return discountValue.min(originalPrice);
        }

        @Override
        public void validateValue(BigDecimal value) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("O valor fixo do desconto deve ser positivo.");
            }
        }
    };

    private final String description;

    public abstract BigDecimal calculate(BigDecimal originalPrice, BigDecimal discountValue);
    
    public abstract void validateValue(BigDecimal value);
}