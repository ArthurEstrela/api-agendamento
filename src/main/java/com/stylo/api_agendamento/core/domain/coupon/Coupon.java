package com.stylo.api_agendamento.core.domain.coupon;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Coupon {

    private UUID id;
    private UUID providerId; // Isolamento: Cupom do salão X só vale no salão X
    
    private String code;     // Normalizado (UPPERCASE, TRIM)
    private DiscountType type;
    private BigDecimal value; // 10 (se for %) ou 15.00 (se for fixed)
    
    private LocalDate expirationDate;
    
    private Integer maxUsages; // Limite global (ex: os 100 primeiros)
    private Integer currentUsages;
    
    private BigDecimal minPurchaseValue; // Regra: Só vale para compras acima de X
    
    private boolean active;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Coupon create(UUID providerId, String code, DiscountType type, BigDecimal value, 
                                LocalDate expirationDate, Integer maxUsages, BigDecimal minPurchaseValue) {
        
        if (providerId == null) throw new BusinessException("O cupom deve pertencer a um estabelecimento.");
        if (code == null || code.isBlank()) throw new BusinessException("O código do cupom é obrigatório.");
        if (type == null) throw new BusinessException("O tipo de desconto é obrigatório.");
        
        // Validação Polimórfica (Delega para o Enum)
        type.validateValue(value);

        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            throw new BusinessException("A data de validade não pode ser no passado.");
        }

        return Coupon.builder()
                .id(UUID.randomUUID())
                .providerId(providerId)
                .code(code.trim().toUpperCase()) // Normalização
                .type(type)
                .value(value)
                .expirationDate(expirationDate)
                .maxUsages(maxUsages)
                .currentUsages(0)
                .minPurchaseValue(minPurchaseValue != null ? minPurchaseValue : BigDecimal.ZERO)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- LÓGICA DE DOMÍNIO ---

    public void validateUsage(BigDecimal purchaseAmount) {
        if (!this.active) {
            throw new BusinessException("Este cupom está inativo.");
        }
        
        if (this.expirationDate != null && LocalDate.now().isAfter(this.expirationDate)) {
            throw new BusinessException("Este cupom expirou em " + this.expirationDate);
        }
        
        if (this.maxUsages != null && this.currentUsages >= this.maxUsages) {
            throw new BusinessException("O limite de utilizações deste cupom foi atingido.");
        }
        
        if (purchaseAmount != null && this.minPurchaseValue != null 
                && purchaseAmount.compareTo(this.minPurchaseValue) < 0) {
            throw new BusinessException("Valor mínimo de compra não atingido. Mínimo: " + this.minPurchaseValue);
        }
    }

    public BigDecimal calculateDiscount(BigDecimal purchaseAmount) {
        // Valida antes de calcular (Garante integridade)
        validateUsage(purchaseAmount);
        
        return this.type.calculate(purchaseAmount, this.value);
    }

    public void incrementUsage() {
        if (this.maxUsages != null && this.currentUsages >= this.maxUsages) {
            throw new BusinessException("Cupom esgotado no momento da aplicação.");
        }
        this.currentUsages++;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    // --- IDENTIDADE ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coupon coupon = (Coupon) o;
        return Objects.equals(id, coupon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}