package com.stylo.api_agendamento.core.domain.coupon;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class Coupon {
    private String id;
    private String providerId; // Isolamento: Cupom do salão X só vale no salão X
    private String code;       // O texto, ex: "BEMVINDO10"
    private DiscountType type;
    private BigDecimal value;  // 10 (se for %) ou 15.00 (se for fixed)
    private LocalDate expirationDate;
    private Integer maxUsages; // Limite global (ex: os 100 primeiros)
    private Integer currentUsages;
    private BigDecimal minPurchaseValue; // Regra: Só vale para compras acima de X
    private boolean active;
    private LocalDateTime createdAt;

    // --- LÓGICA DE DOMÍNIO ---

    public void validate(BigDecimal purchaseAmount) {
        if (!this.active) {
            throw new BusinessException("Este cupom está inativo.");
        }
        if (this.expirationDate != null && LocalDate.now().isAfter(this.expirationDate)) {
            throw new BusinessException("Este cupom expirou.");
        }
        if (this.maxUsages != null && this.currentUsages >= this.maxUsages) {
            throw new BusinessException("Limite de uso deste cupom foi atingido.");
        }
        if (this.minPurchaseValue != null && purchaseAmount.compareTo(this.minPurchaseValue) < 0) {
            throw new BusinessException("Valor mínimo para este cupom não atingido.");
        }
    }

    public BigDecimal calculateDiscount(BigDecimal originalPrice) {
        BigDecimal discountAmount;

        if (this.type == DiscountType.PERCENTAGE) {
            // Valor * (Porcentagem / 100)
            discountAmount = originalPrice.multiply(this.value.divide(new BigDecimal("100")));
        } else {
            discountAmount = this.value;
        }

        // Garante que o desconto não seja maior que o preço (não dar dinheiro pro cliente)
        if (discountAmount.compareTo(originalPrice) > 0) {
            return originalPrice;
        }
        return discountAmount;
    }

    public void incrementUsage() {
        this.currentUsages++;
    }
}