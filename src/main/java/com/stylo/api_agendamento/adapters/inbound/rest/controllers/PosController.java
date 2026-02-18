package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.context.SpringUserContext;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.UserPermission;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.usecases.CreateWalkInUseCase;
import com.stylo.api_agendamento.core.usecases.ProcessPosCheckoutUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Opcional se usar config via SecurityConfig
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/v1/pos")
@RequiredArgsConstructor
@Tag(name = "POS / Comanda Digital", description = "Operações de Balcão: Walk-in e Checkout Presencial")
public class PosController {

    private final CreateWalkInUseCase createWalkInUseCase;
    private final ProcessPosCheckoutUseCase processPosCheckoutUseCase;
    
    // --- 1. Abertura de Comanda / Walk-in ---
    
    @PostMapping("/walk-in")
    @Operation(summary = "Criar Encaixe/Walk-in", description = "Cria um agendamento imediato para cliente presente na loja.")
    public ResponseEntity<Appointment> createWalkIn(@RequestBody @Valid WalkInRequest request) {
        // Nota: A segurança deve garantir que apenas PRO/RECEPTIONIST/MANAGER acessem aqui
        var input = new CreateWalkInUseCase.WalkInInput(
                request.professionalId(),
                request.clientId(),
                request.clientName(),
                request.clientPhone(),
                request.serviceIds()
        );
        return ResponseEntity.ok(createWalkInUseCase.execute(input));
    }

    // --- 2. Checkout / Fechamento de Conta ---

    @PostMapping("/checkout")
    @Operation(summary = "Fechar Conta (Checkout)", description = "Processa pagamento presencial, calcula troco e atualiza caixa.")
    public ResponseEntity<CheckoutResponse> checkout(@RequestBody @Valid CheckoutRequest request) {
        var input = new ProcessPosCheckoutUseCase.CheckoutInput(
                request.appointmentId(),
                request.paymentMethod(),
                request.amountGiven(),
                request.couponCode()
        );

        var result = processPosCheckoutUseCase.execute(input);

        return ResponseEntity.ok(new CheckoutResponse(
                result.appointment().getId(),
                result.appointment().getStatus().name(),
                result.appointment().getFinalPrice(),
                result.change() // Retorna o troco para mostrar na tela
        ));
    }

    // DTOs
    public record WalkInRequest(
            @NotNull String professionalId,
            String clientId,
            String clientName,
            String clientPhone,
            List<String> serviceIds
    ) {}

    public record CheckoutRequest(
            @NotNull String appointmentId,
            @NotNull PaymentMethod paymentMethod,
            BigDecimal amountGiven, // Opcional (se for cartão é null)
            String couponCode
    ) {}

    public record CheckoutResponse(
            String appointmentId,
            String status,
            BigDecimal paidAmount,
            BigDecimal change
    ) {}
}