package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;

import java.math.BigDecimal;
import java.util.UUID;

public interface IPaymentProvider {

    // --- COBRANÇAS ---
    /**
     * Gera um link de pagamento ou PaymentIntent (Checkout).
     * @param destinationAccountId ID da conta conectada (Stripe Connect) que receberá o valor.
     * @param applicationFeeAmount Valor que fica para a plataforma (Sua taxa).
     */
    String createPaymentLink(UUID appointmentId, BigDecimal amount, String destinationAccountId, BigDecimal applicationFeeAmount);

    boolean processDirectPayment(String customerId, BigDecimal amount, String paymentMethodId);

    void refundPayment(String externalPaymentId, BigDecimal amount);

    /**
     * Executa a divisão do dinheiro (Split) após o recebimento.
     */
    void executeTransfer(String sourceChargeId, String targetAccountId, BigDecimal amount);

    // --- ONBOARDING (STRIPE CONNECT) ---
    
    /**
     * Cria o link para o profissional/salão conectar sua conta bancária.
     */
    ConnectOnboardingResult createConnectAccountLink(UUID providerId, String email);

    String continueOnboarding(String stripeAccountId);

    /**
     * Cria um link para o dashboard do Stripe (para o usuário ver saldo/saques).
     */
    String createLoginLink(String stripeAccountId);

    boolean isAccountFullyOnboarded(String stripeAccountId);

    // --- WEBHOOKS ---
    PaymentWebhookInput validateAndParseWebhook(String rawPayload, String signatureHeader);

    // --- DTOs ---
    record ConnectOnboardingResult(String accountId, String onboardingUrl) {}
}