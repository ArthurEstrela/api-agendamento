package com.stylo.api_agendamento.core.usecases.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyCashFlow(
    LocalDate date,
    BigDecimal revenue,
    BigDecimal expenses
) {}