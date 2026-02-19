package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import com.stylo.api_agendamento.core.domain.financial.CashTransactionType;
import com.stylo.api_agendamento.core.ports.IUserContext;
import com.stylo.api_agendamento.core.usecases.ManageCashRegisterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/v1/financial/cash-register")
@RequiredArgsConstructor
@Tag(name = "Controle de Caixa", description = "Abertura, fechamento, sangria e suprimento")
public class CashRegisterController {

    private final ManageCashRegisterUseCase manageCashRegisterUseCase;
    
    // Injeção da Interface (Port) e não da implementação concreta (SpringUserContext)
    private final IUserContext userContext;

    @Operation(summary = "Verifica se o caixa está aberto e retorna saldo atual")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixa está aberto. Retorna os dados."),
            @ApiResponse(responseCode = "404", description = "Nenhum caixa aberto no momento.")
    })
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('finance:read') or hasRole('PROFESSIONAL')")
    public ResponseEntity<CashRegister> getStatus() {
        // Capturamos o providerId aqui porque o método getCurrentStatus do UseCase exige esse parâmetro
        UUID providerId = userContext.getCurrentUser().getProviderId();
        
        // ResponseEntity.of() automaticamente retorna 200 (com o body) ou 404 (se o Optional for empty)
        return ResponseEntity.of(manageCashRegisterUseCase.getCurrentStatus(providerId));
    }

    @Operation(summary = "Abrir caixa", description = "Inicia o caixa do dia com um saldo inicial.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Caixa aberto com sucesso"),
            @ApiResponse(responseCode = "400", description = "Já existe um caixa aberto para o estabelecimento")
    })
    @PostMapping("/open")
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('PROFESSIONAL')")
    public ResponseEntity<CashRegister> open(@RequestBody @Valid OpenRegisterRequest request) {
        // O UseCase já captura o usuário logado internamente, só precisamos repassar os dados do DTO
        CashRegister openedRegister = manageCashRegisterUseCase.openRegister(request.initialBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(openedRegister);
    }

    @Operation(summary = "Fechar caixa", description = "Encerra o caixa do dia, informando o valor real na gaveta e apurando a quebra.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caixa fechado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Não há caixa aberto para fechar ou valor inválido")
    })
    @PostMapping("/close")
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('PROFESSIONAL')")
    public ResponseEntity<CashRegister> close(@RequestBody @Valid CloseRegisterRequest request) {
        // Novamente, o UseCase pega o usuário internamente
        CashRegister closedRegister = manageCashRegisterUseCase.closeRegister(request.finalBalance());
        return ResponseEntity.ok(closedRegister);
    }

    @Operation(summary = "Realizar Sangria ou Suprimento", description = "Adiciona (suprimento) ou retira (sangria) dinheiro do caixa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação registrada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Saldo insuficiente para sangria ou caixa fechado")
    })
    @PostMapping("/operation")
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('PROFESSIONAL')")
    public ResponseEntity<CashRegister> operation(@RequestBody @Valid CashOperationRequest request) {
        CashRegister updatedRegister = manageCashRegisterUseCase.addOperation(
                request.type(), 
                request.amount(), 
                request.description()
        );
        return ResponseEntity.ok(updatedRegister);
    }

    // --- DTOs Internos ---
    
    public record OpenRegisterRequest(
            @NotNull(message = "O saldo inicial é obrigatório.") 
            @PositiveOrZero(message = "O saldo inicial não pode ser negativo.") 
            BigDecimal initialBalance
    ) {}
    
    public record CloseRegisterRequest(
            @NotNull(message = "O saldo final apurado é obrigatório.") 
            @PositiveOrZero(message = "O saldo final apurado não pode ser negativo.") 
            BigDecimal finalBalance
    ) {}
    
    public record CashOperationRequest(
            @NotNull(message = "O tipo de operação (BLEED ou SUPPLY) é obrigatório.") 
            CashTransactionType type,
            
            @NotNull(message = "O valor da operação é obrigatório.") 
            @PositiveOrZero(message = "O valor da operação deve ser positivo.") 
            BigDecimal amount,
            
            String description
    ) {}
}