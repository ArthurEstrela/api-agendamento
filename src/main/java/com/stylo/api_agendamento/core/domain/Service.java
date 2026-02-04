package com.stylo.api_agendamento.core.domain;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Service {
    private String id;
    private String name;
    private String description;
    private Integer duration; // em minutos
    private BigDecimal price;
}