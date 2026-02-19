package com.stylo.api_agendamento.adapters.outbound.persistence.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;

@Entity
@Table(name = "products")
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProductEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // ✨ Atualizado de String para UUID

    @Column(name = "service_provider_id", nullable = false)
    private UUID serviceProviderId; // ✨ Atualizado para UUID

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ✨ Precisão monetária
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price; // Preço de venda

    // ✨ Sincronizado com o Domínio: Preço de Custo para Dashboard Financeiro
    @Column(name = "cost_price", precision = 19, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // ✨ Sincronizado com o Domínio: Alerta de reposição
    @Column(name = "min_stock_alert")
    private Integer minStockAlert;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}