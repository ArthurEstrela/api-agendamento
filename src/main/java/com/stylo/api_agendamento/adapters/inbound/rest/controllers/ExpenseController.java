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
    public ResponseEntity<Expense> create(@RequestBody @Valid CreateExpenseRequest request) {
        // O mapeamento seria feito aqui para o objeto de domínio Expense
        // E salvo via repositório ou UseCase específico de criação
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}