package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.financial.FinancialDashboardResponse;
import com.stylo.api_agendamento.core.ports.IUserContext;
import com.stylo.api_agendamento.core.usecases.ConnectProviderUseCase;
import com.stylo.api_agendamento.core.usecases.GetFinancialDashboardUseCase;
import com.stylo.api_agendamento.core.usecases.GetOccupancyReportUseCase;
import com.stylo.api_agendamento.core.usecases.dto.DailyCashFlow;
import com.stylo.api_agendamento.core.usecases.dto.OccupancyReport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/financial")
@RequiredArgsConstructor
@Tag(name = "Financeiro", description = "Gestão financeira, relatórios e configuração de recebimentos (Stripe)")
public class FinancialController {

    private final GetFinancialDashboardUseCase getFinancialDashboardUseCase;
    private final GetOccupancyReportUseCase getOccupancyReportUseCase;
    private final ConnectProviderUseCase connectProviderUseCase;
    
    // Injeção da porta (Clean Architecture) e não do contexto concreto do Spring
    private final IUserContext userContext;

    @Operation(summary = "Obter Dashboard Financeiro", description = "Retorna o resumo de receitas, despesas e lucro do estabelecimento num determinado período.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard financeiro gerado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    @GetMapping("/dashboard") // O providerId foi retirado da rota por segurança
    @PreAuthorize("hasAuthority('finance:read') or hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<FinancialDashboardResponse> getDashboard(
            @RequestParam @NotNull(message = "A data de início é obrigatória") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull(message = "A data de fim é obrigatória") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // ✨ CORREÇÃO DE SEGURANÇA E TIPAGEM: Provider ID extraído nativamente via UUID e Token
        UUID providerId = userContext.getCurrentUser().getProviderId();

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

    @Operation(summary = "Relatório de Ocupação", description = "Retorna o relatório de taxa de ocupação da agenda do profissional logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Relatório de ocupação gerado com sucesso")
    })
    @GetMapping("/occupancy-report")
    @PreAuthorize("hasAuthority('appointment:read') or hasRole('PROFESSIONAL')")
    public ResponseEntity<OccupancyReport> getOccupancyReport(
            @RequestParam @NotNull(message = "A data de início é obrigatória") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @NotNull(message = "A data de fim é obrigatória") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // ✨ CORREÇÃO: Utilizando a porta de contexto ao invés do @AuthenticationPrincipal
        UUID professionalId = userContext.getCurrentUserId();

        return ResponseEntity.ok(getOccupancyReportUseCase.execute(professionalId, startDate, endDate));
    }

    @Operation(summary = "Obter link da conta bancária (Stripe Connect)", description = "Retorna uma URL segura. Se o salão não tem conta, cria. Se já tem, leva para o painel de transferências e saldo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Link de integração gerado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Apenas o dono do estabelecimento pode aceder a isto")
    })
    @PostMapping("/onboarding-link")
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')") // Segurança máxima
    public ResponseEntity<Map<String, String>> getOnboardingLink() {
        
        // ✨ CORREÇÃO DE TIPAGEM: String -> UUID
        UUID providerId = userContext.getCurrentUser().getProviderId();

        String redirectUrl = connectProviderUseCase.execute(providerId);

        return ResponseEntity.ok(Map.of("url", redirectUrl));
    }
}