package com.stylo.api_agendamento.adapters.inbound.rest.dto.financial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stylo.api_agendamento.core.domain.Expense.ExpenseFrequency;
import com.stylo.api_agendamento.core.domain.Expense.ExpenseType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateExpenseRequest(
        @NotBlank(message = "A descrição é obrigatória.") String description,

        @NotNull(message = "O valor é obrigatório.") @Positive(message = "O valor da despesa deve ser positivo.") BigDecimal amount,

        @NotNull(message = "A data de competência é obrigatória.") LocalDateTime date,

        @NotBlank(message = "A categoria é obrigatória. (Ex: ALUGUER, LUZ, PRODUTOS)") String category,

        // Novos campos mapeados a partir da entidade de domínio
        ExpenseType type,
        ExpenseFrequency frequency) {
}