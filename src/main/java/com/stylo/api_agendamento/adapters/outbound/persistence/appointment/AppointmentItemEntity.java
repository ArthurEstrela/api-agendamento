package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "appointment_items")
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id; // ✨ Corrigido de String para UUID

    // ✨ DESACOPLAMENTO DDD: Apenas o ID do produto. 
    // Impede joins lentos e mantêm a comanda isolada do catálogo.
    @Column(name = "product_id", nullable = false)
    private UUID productId; 

    // Snapshot do Nome do Produto (Caso o dono do salão exclua o produto do catálogo, 
    // a comanda antiga não perde o nome do que foi vendido)
    @Column(name = "product_name", nullable = false)
    private String productName;

    // O preço CONGELADO no momento da venda
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;
}