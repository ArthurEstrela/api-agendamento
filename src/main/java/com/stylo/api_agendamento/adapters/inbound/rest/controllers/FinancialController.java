package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.context.SpringUserContext;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.financial.FinancialDashboardResponse;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.usecases.ConnectProviderUseCase;
import com.stylo.api_agendamento.core.usecases.GetFinancialDashboardUseCase;
import com.stylo.api_agendamento.core.usecases.GetOccupancyReportUseCase;
import com.stylo.api_agendamento.core.usecases.dto.DailyCashFlow;
import com.stylo.api_agendamento.core.usecases.dto.OccupancyReport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/financial")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Gestão financeira, relatórios e configuração de recebimentos")
public class FinancialController {

    private final GetFinancialDashboardUseCase getFinancialDashboardUseCase;
    private final GetOccupancyReportUseCase getOccupancyReportUseCase;
    private final ConnectProviderUseCase connectProviderUseCase;
    private final SpringUserContext userContext;

    @GetMapping("/{providerId}/dashboard")
    public ResponseEntity<FinancialDashboardResponse> getDashboard(
            @PathVariable String providerId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        var dashboard = getFinancialDashboardUseCase.execute(providerId, startDate, endDate);

        List<FinancialDashboardResponse.DailyCashFlowResponse> dailyFlowResponse = dashboard.dailyFlow()
                .stream()
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
            @AuthenticationPrincipal User user, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(getOccupancyReportUseCase.execute(
                user.getId(), 
                startDate,
                endDate));
    }

    @PostMapping("/onboarding-link")
    @Operation(summary = "Obter link da conta bancária (Stripe)", description = "Retorna uma URL segura. Se o usuário não tem conta, cria. Se já tem, leva para o painel de saldo.")
    public ResponseEntity<Map<String, String>> getOnboardingLink() {
        // ✨ CORREÇÃO: Usamos getCurrentUser() que acabamos de criar
        String providerId = userContext.getCurrentUser().getProviderId();

        String redirectUrl = connectProviderUseCase.execute(providerId);

        return ResponseEntity.ok(Map.of("url", redirectUrl));
    }
}