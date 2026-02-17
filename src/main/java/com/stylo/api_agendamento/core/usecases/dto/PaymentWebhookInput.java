package com.stylo.api_agendamento.core.usecases.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record PaymentWebhookInput(
    String eventId,               // ID único do evento (crucial para Idempotência/Dedup)
    String gatewayPaymentId,      // O ID da transação no Stripe/MP (ex: pi_3Mej...)
    String eventType,             // O tipo de evento (ex: "checkout.session.completed")
    String status,                // O status normalizado (ex: "succeeded", "failed")
    BigDecimal amount,            // ✨ Segurança: Validar se o valor pago bate com o preço do serviço
    Map<String, String> metadata, // ✨ O Segredo: Carrega IDs de agendamento, provider, user, etc.
    Instant eventTimestamp        // Quando ocorreu (para não processar eventos velhos)
) {
    // Construtor compacto para garantir que metadata nunca seja null
    public PaymentWebhookInput {
        if (metadata == null) {
            metadata = Collections.emptyMap();
        }
    }
}