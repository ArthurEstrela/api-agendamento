package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.financial.CreateExpenseRequest;
import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final IFinancialRepository financialRepository;

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestHeader("X-Provider-ID") String providerId, 
            @RequestBody @Valid CreateExpenseRequest request) {
        
        // Mapeamento corrigido para usar .providerId() conforme o domínio
        Expense expense = Expense.builder()
                .description(request.description())
                .amount(request.amount())
                .date(request.date())
                .category(request.category())
                .providerId(providerId)
                .build();

        // Como o port retorna void, executamos a ação e retornamos Created
        financialRepository.saveExpense(expense);
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}