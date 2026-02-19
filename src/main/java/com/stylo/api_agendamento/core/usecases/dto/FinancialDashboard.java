package com.stylo.api_agendamento.core.usecases.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO consolidado para alimentar o Dashboard Financeiro no front-end.
 */
public record FinancialDashboard(
    BigDecimal totalRevenue,
    BigDecimal totalExpenses,
    BigDecimal netProfit,
    List<DailyCashFlow> dailyFlow
) {}