package com.stylo.api_agendamento.core.usecases.dto;

public record PaymentWebhookInput(
    String providerId,
    String externalReference, // ID da transação no Stripe
    String status,             // "succeeded", "failed", etc.
    String eventType           // "checkout.session.completed", "invoice.paid"
) {}