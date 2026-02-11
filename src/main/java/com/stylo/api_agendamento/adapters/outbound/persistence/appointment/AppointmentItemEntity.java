package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import com.stylo.api_agendamento.adapters.outbound.persistence.product.ProductEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "appointment_items")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Relacionamento ManyToOne: Vários itens pertencem a um produto
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    // O preço CONGELADO no momento da venda (Snapshot)
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    // Opcional: nome do produto congelado (caso deletem o produto original)
    @Column(name = "product_name")
    private String productName;
}