package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.context.SpringUserContext;
import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import com.stylo.api_agendamento.core.domain.financial.CashTransactionType;
import com.stylo.api_agendamento.core.usecases.ManageCashRegisterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/v1/financial/cash-register")
@RequiredArgsConstructor
@Tag(name = "Controle de Caixa", description = "Abertura, fechamento, sangria e suprimento")
public class CashRegisterController {

    private final ManageCashRegisterUseCase manageCashRegisterUseCase;
    private final SpringUserContext userContext;

    @GetMapping("/status")
    @Operation(summary = "Verifica se o caixa está aberto e retorna saldo atual")
    public ResponseEntity<CashRegister> getStatus() {
        var user = userContext.getCurrentUser();
        return ResponseEntity.of(manageCashRegisterUseCase.getCurrentStatus(user.getProviderId()));
    }

    @PostMapping("/open")
    @Operation(summary = "Abrir caixa")
    public ResponseEntity<CashRegister> open(@RequestBody @Valid OpenRegisterRequest request) {
        var user = userContext.getCurrentUser();
        return ResponseEntity.ok(manageCashRegisterUseCase.openRegister(user, request.initialBalance()));
    }

    @PostMapping("/close")
    @Operation(summary = "Fechar caixa")
    public ResponseEntity<CashRegister> close(@RequestBody @Valid CloseRegisterRequest request) {
        var user = userContext.getCurrentUser();
        return ResponseEntity.ok(manageCashRegisterUseCase.closeRegister(user, request.finalBalance()));
    }

    @PostMapping("/operation")
    @Operation(summary = "Realizar Sangria ou Suprimento")
    public ResponseEntity<CashRegister> operation(@RequestBody @Valid CashOperationRequest request) {
        var user = userContext.getCurrentUser();
        return ResponseEntity.ok(manageCashRegisterUseCase.addOperation(
                user, 
                request.type(), 
                request.amount(), 
                request.description()
        ));
    }

    // DTOs internos para simplicidade (idealmente em arquivos separados)
    public record OpenRegisterRequest(@NotNull @PositiveOrZero BigDecimal initialBalance) {}
    public record CloseRegisterRequest(@NotNull @PositiveOrZero BigDecimal finalBalance) {}
    public record CashOperationRequest(
            @NotNull CashTransactionType type,
            @NotNull @PositiveOrZero BigDecimal amount,
            String description
    ) {}
}