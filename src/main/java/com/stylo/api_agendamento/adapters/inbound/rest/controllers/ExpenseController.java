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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Void> create(@RequestBody @Valid CreateExpenseRequest request) {
        
        // 1. Extração Segura: Identidade baseada no Token JWT (inextraível/impossível de forjar)
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
        financialRepository.saveExpense(expense);
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}