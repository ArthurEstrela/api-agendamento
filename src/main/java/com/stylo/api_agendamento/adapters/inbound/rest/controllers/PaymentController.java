// src/main/java/com/stylo/api_agendamento/adapters/inbound/rest/controllers/PaymentController.java
package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import com.stylo.api_agendamento.core.usecases.HandlePaymentWebhookUseCase;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final HandlePaymentWebhookUseCase handlePaymentWebhookUseCase;
    private final IPaymentProvider paymentProvider;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature, // Ou X-Signature dependendo do gateway
            @RequestBody String rawPayload // ✨ Recebe o JSON bruto para validação
    ) {
        try {
            // 1. O Provider valida a assinatura e converte para nosso DTO interno
            // Isso isola o Controller da biblioteca específica (Stripe/MP)
            PaymentWebhookInput input = paymentProvider.validateAndParseWebhook(rawPayload, signature);

            // 2. Chama o caso de uso
            handlePaymentWebhookUseCase.execute(input);

            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.error("Assinatura de webhook inválida: {}", e.getMessage());
            return ResponseEntity.status(403).build(); // Forbidden
        } catch (Exception e) {
            log.error("Erro processando webhook: ", e);
            // Retornar 500 faz o gateway tentar de novo (retry strategy)
            return ResponseEntity.internalServerError().build();
        }
    }
}