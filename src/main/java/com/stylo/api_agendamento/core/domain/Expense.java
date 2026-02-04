package com.stylo.api_agendamento.core.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Expense {
    private String id;
    private String providerId;
    private String description;
    private BigDecimal amount;
    private LocalDateTime date;
    private String category;
    private String type; // one-time ou recurring
}