package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Product {

    private UUID id;
    private UUID serviceProviderId;

    private String name;
    private String description;
    
    // Preço de Venda
    private BigDecimal price;
    
    // Preço de Custo (Para cálculo de lucro)
    private BigDecimal costPrice;

    private Integer stockQuantity;
    private Integer minStockAlert; // Gatilho para notificação de estoque baixo
    
    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Product create(UUID serviceProviderId, String name, String description, 
                                 BigDecimal price, BigDecimal costPrice, 
                                 Integer initialStock, Integer minStockAlert) {
        
        if (serviceProviderId == null) {
            throw new BusinessException("O produto deve pertencer a um prestador de serviços.");
        }
        if (name == null || name.isBlank()) {
            throw new BusinessException("O nome do produto é obrigatório.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O preço de venda não pode ser negativo.");
        }

        return Product.builder()
                .id(UUID.randomUUID()) // Identidade gerada
                .serviceProviderId(serviceProviderId)
                .name(name)
                .description(description)
                .price(price)
                .costPrice(costPrice != null ? costPrice : BigDecimal.ZERO)
                .stockQuantity(initialStock != null ? initialStock : 0)
                .minStockAlert(minStockAlert)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void update(String name, String description, BigDecimal price, BigDecimal costPrice, Integer minStockAlert) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) { // Permite limpar a descrição passando string vazia
            this.description = description;
        }
        
        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("O preço de venda não pode ser negativo.");
            }
            this.price = price;
        }

        if (costPrice != null) {
            if (costPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("O preço de custo não pode ser negativo.");
            }
            this.costPrice = costPrice;
        }

        if (minStockAlert != null) {
            this.minStockAlert = minStockAlert;
        }

        this.updatedAt = LocalDateTime.now();
    }

    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("A quantidade para baixa deve ser positiva.");
        }
        if (this.stockQuantity < quantity) {
            throw new BusinessException("Estoque insuficiente para o produto: " + this.name);
        }
        this.stockQuantity -= quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void addStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("A quantidade para reposição deve ser positiva.");
        }
        this.stockQuantity += quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    // --- LÓGICA DE DOMÍNIO ---

    public boolean isBelowMinStock() {
        return this.isActive && 
               this.minStockAlert != null && 
               this.stockQuantity <= this.minStockAlert;
    }

    public BigDecimal calculateProfitMargin() {
        if (this.price == null || this.price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        // (Preço - Custo)
        BigDecimal profit = this.price.subtract(this.costPrice != null ? this.costPrice : BigDecimal.ZERO);
        return profit;
    }

    public BigDecimal calculateProfitMarginPercentage() {
        if (this.price == null || this.price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal profit = calculateProfitMargin();
        // (Lucro / Preço) * 100
        return profit.divide(this.price, 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100));
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}