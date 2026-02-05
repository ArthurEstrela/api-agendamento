package com.stylo.api_agendamento.adapters.outbound.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "expenses")
@Data
public class ExpenseEntity {
    @Id
    private String id;
    private String providerId;
    private String description;
    private BigDecimal amount;
    private LocalDateTime date;
    private String category; // Ex: "Aluguel", "Material"
    private String type; // "one-time" ou "recurring"
}