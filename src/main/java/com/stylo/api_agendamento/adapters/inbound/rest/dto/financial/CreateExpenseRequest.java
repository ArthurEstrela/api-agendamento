package com.stylo.api_agendamento.adapters.inbound.rest.dto.financial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExpenseRequest(
    @NotBlank String description,
    @NotNull BigDecimal amount,
    @NotNull LocalDateTime date,
    @NotBlank String category // EX: ALUGUEL, PRODUTOS, LUZ
) {}