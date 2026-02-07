package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.HandlePaymentWebhookUseCase;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final HandlePaymentWebhookUseCase handlePaymentWebhookUseCase;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-Provider-ID") String providerId, // ID vindo no header ou payload
            @RequestBody Map<String, Object> payload) {
        
        // Converte o payload genérico do gateway para o DTO do Use Case
        var input = new PaymentWebhookInput(
            providerId,
            (String) payload.get("type"),
            (String) ((Map<?, ?>) payload.get("data")).get("status"),
            payload.toString()
        );

        handlePaymentWebhookUseCase.execute(input);
        return ResponseEntity.ok().build();
    }
}