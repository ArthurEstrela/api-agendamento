package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Expense {

    private UUID id;
    private UUID providerId;

    private String description;
    private BigDecimal amount;
    
    // Data de competência ou vencimento
    private LocalDateTime date; 
    
    // Poderia ser um Enum se as categorias fossem fixas, mas String permite flexibilidade ao usuário
    private String category; 

    private ExpenseType type;
    private ExpenseFrequency frequency;

    // ✨ NOVO: Controle de pagamento
    private boolean paid;
    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Expense create(UUID providerId, String description, BigDecimal amount, 
                                 LocalDateTime date, String category, 
                                 ExpenseType type, ExpenseFrequency frequency) {
        
        // 1. Validações Básicas
        if (providerId == null) throw new BusinessException("A despesa deve estar vinculada a um prestador.");
        if (description == null || description.isBlank()) throw new BusinessException("A descrição é obrigatória.");
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor da despesa deve ser positivo.");
        }

        // 2. Validação de Recorrência
        if (type == ExpenseType.RECURRING && frequency == null) {
            throw new BusinessException("Despesas recorrentes exigem uma frequência definida.");
        }

        // 3. Construção
        return Expense.builder()
                .id(UUID.randomUUID()) // Identidade gerada
                .providerId(providerId)
                .description(description)
                .amount(amount)
                .date(date != null ? date : LocalDateTime.now()) // Permite data retroativa ou futura
                .category(category != null ? category : "Outros")
                .type(type != null ? type : ExpenseType.ONE_TIME)
                .frequency(type == ExpenseType.RECURRING ? frequency : null) // Limpa frequência se não for recorrente
                .paid(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void update(String description, BigDecimal amount, LocalDateTime date, String category) {
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
        
        if (amount != null) {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("O novo valor deve ser positivo.");
            }
            this.amount = amount;
        }

        if (date != null) {
            this.date = date;
        }

        if (category != null && !category.isBlank()) {
            this.category = category;
        }

        this.updatedAt = LocalDateTime.now();
    }

    public void markAsPaid() {
        if (this.paid) return; // Idempotente
        
        this.paid = true;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsUnpaid() {
        if (!this.paid) return;

        this.paid = false;
        this.paidAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isRecurring() {
        return this.type == ExpenseType.RECURRING;
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Expense expense = (Expense) o;
        return Objects.equals(id, expense.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- ENUMS ---

    public enum ExpenseType {
        ONE_TIME,   // Despesa única
        RECURRING   // Despesa fixa/recorrente
    }

    public enum ExpenseFrequency {
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}