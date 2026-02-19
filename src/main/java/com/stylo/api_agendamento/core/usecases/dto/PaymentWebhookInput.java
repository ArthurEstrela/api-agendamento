package com.stylo.api_agendamento.core.usecases.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record PaymentWebhookInput(
    String eventId,               // ✨ String: ID externo do evento (ex: evt_1...)
    String gatewayPaymentId,      // ✨ String: ID da transação no Stripe (ex: pi_3...)
    String eventType,             // O tipo de evento (ex: "checkout.session.completed")
    String status,                // O status normalizado (ex: "succeeded", "failed")
    BigDecimal amount,            // Validar se o valor pago bate com o preço do serviço
    Map<String, String> metadata, // Carrega UUIDs internos serializados como Strings
    Instant eventTimestamp        // Para evitar processamento de webhooks atrasados
) {
    // Construtor compacto garantindo Null Safety no metadata
    public PaymentWebhookInput {
        if (metadata == null) {
            metadata = Collections.emptyMap();
        }
    }
}