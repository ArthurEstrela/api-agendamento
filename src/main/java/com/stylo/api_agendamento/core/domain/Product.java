package com.stylo.api_agendamento.core.domain;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Product {
    private String id;
    private String serviceProviderId;
    private String name;
    private String description;
    private BigDecimal price;
    
    // ✨ Atualização: Campos alinhados com V6__create_products_table.sql
    private BigDecimal costPrice; // Para calcular lucro real
    private Integer stockQuantity;
    private Integer minStockAlert; // O gatilho do alerta
    private Boolean isActive;

    public void deductStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalArgumentException("Estoque insuficiente para o produto: " + this.name);
        }
        this.stockQuantity -= quantity;
    }

    public void restoreStock(int quantity) {
        if (quantity <= 0) return;
        this.stockQuantity += quantity;
    }

    // ✨ Nova Lógica de Domínio
    public boolean isBelowMinStock() {
        return this.minStockAlert != null && this.stockQuantity <= this.minStockAlert;
    }
}