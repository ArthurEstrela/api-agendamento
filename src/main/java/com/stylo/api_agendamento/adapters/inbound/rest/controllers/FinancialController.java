package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.financial.FinancialDashboardResponse;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.usecases.GetFinancialDashboardUseCase;
import com.stylo.api_agendamento.core.usecases.GetOccupancyReportUseCase;
import com.stylo.api_agendamento.core.usecases.dto.DailyCashFlow; // Certifique-se de importar o DTO do UseCase
import com.stylo.api_agendamento.core.usecases.dto.OccupancyReport;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/financial")
@RequiredArgsConstructor
public class FinancialController {

    private final GetFinancialDashboardUseCase getFinancialDashboardUseCase;
    private final GetOccupancyReportUseCase getOccupancyReportUseCase;

    @GetMapping("/{providerId}/dashboard")
    public ResponseEntity<FinancialDashboardResponse> getDashboard(
            @PathVariable String providerId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        var dashboard = getFinancialDashboardUseCase.execute(providerId, startDate, endDate);

        // Tipagem explícita no Stream para resolver o erro de inferência <R>
        List<FinancialDashboardResponse.DailyCashFlowResponse> dailyFlowResponse = dashboard.dailyFlow().stream()
                .map((DailyCashFlow flow) -> new FinancialDashboardResponse.DailyCashFlowResponse(
                        flow.date(),
                        flow.revenue(),
                        flow.expenses()))
                .toList();

        var response = new FinancialDashboardResponse(
                dashboard.totalRevenue(),
                dashboard.totalExpenses(),
                dashboard.netProfit(),
                dailyFlowResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/occupancy-report")
    public ResponseEntity<OccupancyReport> getOccupancyReport(
            @AuthenticationPrincipal User user, // ✨ Pega o ProviderId do dono logado
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(getOccupancyReportUseCase.execute(
                user.getId(), // Assumindo que o ID do User é o ProviderId para o dono
                startDate,
                endDate));
    }
}