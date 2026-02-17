// Arquivo: src/main/java/com/stylo/api_agendamento/core/ports/IPaymentProvider.java
package com.stylo.api_agendamento.core.ports;

import java.math.BigDecimal;

import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;

public interface IPaymentProvider {
    boolean processPayment(String customerId, BigDecimal amount, String paymentMethodId);

    String generatePaymentLink(String appointmentId, BigDecimal amount);

    void refund(String externalPaymentId, BigDecimal amount);

    void executeSplit(String paymentId, String targetAccountId, BigDecimal profAmount, BigDecimal providerAmount);

    PaymentWebhookInput validateAndParseWebhook(String rawPayload, String signatureHeader);
}