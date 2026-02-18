package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import java.math.BigDecimal;

public interface IPaymentProvider {

    // Agora aceita destinationAccountId (ID do barbeiro) e feePercentage (sua taxa)
    String generatePaymentLink(String appointmentId, BigDecimal amount, String destinationAccountId,
            BigDecimal feePercentage);

    boolean processPayment(String customerId, BigDecimal amount, String paymentMethodId);

    void refund(String externalPaymentId, BigDecimal amount);

    void executeSplit(String paymentId, String targetAccountId, BigDecimal profAmount, BigDecimal providerAmount);

    // ✨ Mudança Crítica: Retorna um Record com ID e URL
    ConnectOnboardingResult createConnectAccountLink(String providerId, String email);

    boolean isAccountFullyOnboarded(String stripeAccountId);

    PaymentWebhookInput validateAndParseWebhook(String rawPayload, String signatureHeader);

    // DTO simples para o retorno do onboarding
    record ConnectOnboardingResult(String accountId, String onboardingUrl) {
    }

    String continueOnboarding(String stripeAccountId);

    String createLoginLink(String stripeAccountId);
}