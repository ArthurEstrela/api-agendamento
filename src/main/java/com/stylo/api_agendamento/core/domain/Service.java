package com.stylo.api_agendamento.core.domain;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Service {
    private String id;
    private String name;
    private String description;
    private Integer duration; // Crucial para o backend validar horários
    private BigDecimal price;
}