package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "services")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceEntity {
    @Id
    private String id;
    private String name;
    private String description;
    private Integer duration; // Alinhado com duration: number no TS
    private BigDecimal price; // Alinhado com price: number no TS
    private String providerId; // Relacionamento com o ServiceProvider
}