package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.usecases.CreateWalkInUseCase;
import com.stylo.api_agendamento.core.usecases.ProcessPosCheckoutUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/pos")
@RequiredArgsConstructor
@Tag(name = "POS / Comanda Digital", description = "Operações de Balcão: Encaixes (Walk-in) e Checkout Presencial")
public class PosController {

    private final CreateWalkInUseCase createWalkInUseCase;
    private final ProcessPosCheckoutUseCase processPosCheckoutUseCase;
    
    // --- 1. Abertura de Comanda / Walk-in ---
    
    @Operation(summary = "Criar Encaixe/Walk-in", description = "Cria um agendamento imediato para um cliente que chega fisicamente ao estabelecimento.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comanda de Walk-in criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou profissional indisponível")
    })
    @PostMapping("/walk-in")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<Appointment> createWalkIn(@RequestBody @Valid WalkInRequest request) {
        
        // Tratamento seguro: Num "walk-in" o cliente pode não ter registo na app, logo o clientId pode vir nulo.
        UUID parsedClientId = (request.clientId() != null && !request.clientId().isBlank()) 
                ? UUID.fromString(request.clientId()) 
                : null;

        // Correção de nomenclatura (WalkInInput -> Input) e de tipagem (String -> UUID)
        var input = new CreateWalkInUseCase.Input(
                UUID.fromString(request.professionalId()),
                parsedClientId,
                request.clientName(),
                request.clientPhone(),
                request.serviceIds().stream().map(UUID::fromString).collect(Collectors.toList())
        );
        
        return ResponseEntity.ok(createWalkInUseCase.execute(input));
    }

    // --- 2. Checkout / Fecho de Conta ---

    @Operation(summary = "Fechar Conta (Checkout)", description = "Processa o pagamento presencial no balcão, calcula o troco, aplica descontos/cupões e atualiza a gaveta do caixa físico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout concluído com sucesso"),
            @ApiResponse(responseCode = "400", description = "Valor entregue insuficiente ou regras de negócio violadas (ex: comanda já paga)")
    })
    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('PROFESSIONAL')")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody @Valid CheckoutRequest request) {
        
        // Correção de nomenclatura (CheckoutInput -> Input) e tipagem
        var input = new ProcessPosCheckoutUseCase.Input(
                UUID.fromString(request.appointmentId()),
                request.paymentMethod(),
                request.amountGiven(),
                request.couponCode()
        );

        ProcessPosCheckoutUseCase.Response result = processPosCheckoutUseCase.execute(input);

        // Retorna os dados mapeados para o ecrã do PDV do balcão (mostrando o troco calculado)
        return ResponseEntity.ok(new CheckoutResponse(
                result.appointment().getId(),
                result.appointment().getStatus().name(),
                result.appointment().getFinalPrice(),
                result.change()
        ));
    }

    // --- DTOs Internos ---

    public record WalkInRequest(
            @NotBlank(message = "O ID do profissional é obrigatório.") 
            String professionalId,
            
            String clientId, // Opcional
            
            @NotBlank(message = "O nome do cliente é obrigatório para registar um encaixe de balcão.") 
            String clientName,
            
            String clientPhone, // Opcional
            
            @NotNull(message = "Deve selecionar pelo menos um serviço.") 
            List<String> serviceIds
    ) {}

    public record CheckoutRequest(
            @NotBlank(message = "O ID da comanda/agendamento é obrigatório.") 
            String appointmentId,
            
            @NotNull(message = "O método de pagamento é obrigatório.") 
            PaymentMethod paymentMethod,
            
            BigDecimal amountGiven, // Opcional (se for cartão de crédito/débito, não há troco)
            
            String couponCode // Opcional
    ) {}

    public record CheckoutResponse(
            UUID appointmentId,
            String status,
            BigDecimal paidAmount,
            BigDecimal change
    ) {}
}