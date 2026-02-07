package com.stylo.api_agendamento.adapters.inbound.rest.dto.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinancialDashboardResponse(
    BigDecimal totalRevenue,
    BigDecimal totalExpenses,
    BigDecimal netProfit,
    List<DailyCashFlowResponse> dailyFlow
) {
    // Mova o record para dentro para permitir a referência estática
    public record DailyCashFlowResponse(
        LocalDate date,
        BigDecimal revenue,
        BigDecimal expense
    ) {}
}