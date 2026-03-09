package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.financial.CreateExpenseRequest;
import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/expenses")
@RequiredArgsConstructor
@Tag(name = "Despesas Financeiras", description = "Gestão de contas a pagar e despesas do estabelecimento")
public class ExpenseController {

    private final IFinancialRepository financialRepository;
    
    // Injeção da porta do contexto para extração segura de dados do utilizador logado
    private final IUserContext userContext;

    @Operation(summary = "Criar Despesa", description = "Regista uma nova despesa (única ou recorrente) associada ao estabelecimento logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Despesa criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou regra de negócio violada")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Expense> create(@RequestBody @Valid CreateExpenseRequest request) {
        
        // 1. Extração Segura: Identidade baseada no Token JWT (Substitui o providerId do payload por segurança)
        UUID providerId = userContext.getCurrentUser().getProviderId();

        // 2. Factory do Domínio: Aciona o Expense.create() para aplicar todas as validações (DDD)
        Expense expense = Expense.create(
                providerId,
                request.description(),
                request.amount(),
                request.date(),
                request.category(),
                request.type(),
                request.frequency()
        );

        // 3. Persistência
        Expense savedExpense = financialRepository.saveExpense(expense);
        
        // Retornamos a despesa salva para o frontend poder atualizar a lista em tempo real
        return ResponseEntity.status(HttpStatus.CREATED).body(savedExpense);
    }

    @Operation(summary = "Listar Despesas", description = "Retorna a lista de despesas do estabelecimento num período de data.")
    @GetMapping // ✨ CORREÇÃO: Removido o /provider/{providerId} da rota. Fica apenas GET /v1/expenses
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER') or hasRole('PROFESSIONAL')")
    public ResponseEntity<List<Expense>> listExpenses(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 1. Extração Segura Direto do Token (Garante que nunca haverá IDOR - Acesso a dados de terceiros)
        UUID providerId = userContext.getCurrentUser().getProviderId();

        // 2. Tratamento do Período (Se o front não mandar datas, assume o mês atual por padrão)
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : YearMonth.now().atEndOfMonth().atTime(LocalTime.MAX);

        // 3. Busca no repositório
        List<Expense> expenses = financialRepository.findExpensesByProviderAndPeriod(providerId, start, end);

        return ResponseEntity.ok(expenses);
    }

    @Operation(summary = "Remover Despesa", description = "Exclui uma despesa existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Despesa excluída com sucesso")
    })
    @DeleteMapping("/{expenseId}")
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> deleteExpense(@PathVariable UUID expenseId) {
        
        financialRepository.deleteExpense(expenseId);
        
        return ResponseEntity.noContent().build();
    }
}