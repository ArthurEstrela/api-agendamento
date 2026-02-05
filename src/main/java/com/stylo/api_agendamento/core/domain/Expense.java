package com.stylo.api_agendamento.core.domain;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Expense {
    private String id;
    private String providerId;
    private String description;
    private BigDecimal amount;
    private LocalDateTime date;
    private String category; // Ex: "Aluguel", "Produtos"
    private String type; // "one-time" | "recurring"
    private String frequency; // "monthly"
}