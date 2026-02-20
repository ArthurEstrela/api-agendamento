package com.stylo.api_agendamento.adapters.outbound.payment;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException; // ✨ IMPORT ADICIONADO AQUI
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class StripePaymentAdapter implements IPaymentProvider {

    @Value("${stripe.api.key}")
    private String apiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }

    @Override
    public PaymentWebhookInput validateAndParseWebhook(String payload, String signature) {
        try {
            // 1. O Coração da Segurança: Valida a assinatura HMAC SHA-256
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);

            String eventId = event.getId();
            String eventType = event.getType();
            Instant timestamp = Instant.ofEpochSecond(event.getCreated());

            String gatewayPaymentId = null;
            String status = "unknown";
            BigDecimal amount = BigDecimal.ZERO;
            Map<String, String> metadata = new HashMap<>();

            // 2. Extrai os dados baseado no tipo de objeto que o Stripe enviou
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            if (dataObjectDeserializer.getObject().isPresent()) {
                StripeObject stripeObject = dataObjectDeserializer.getObject().get();

                // Caso o pagamento seja via PaymentIntent direto
                if (stripeObject instanceof PaymentIntent paymentIntent) {
                    gatewayPaymentId = paymentIntent.getId();
                    status = paymentIntent.getStatus();
                    amount = BigDecimal.valueOf(paymentIntent.getAmount() / 100.0); // Converte de centavos para Real
                    metadata = paymentIntent.getMetadata();
                } 
                // Caso o pagamento seja via Checkout Session (Mais comum em assinaturas SaaS)
                else if (stripeObject instanceof Session session) {
                    gatewayPaymentId = session.getPaymentIntent();
                    status = session.getPaymentStatus();
                    amount = session.getAmountTotal() != null ? 
                             BigDecimal.valueOf(session.getAmountTotal() / 100.0) : BigDecimal.ZERO;
                    metadata = session.getMetadata();
                }
            }

            return new PaymentWebhookInput(
                    eventId, gatewayPaymentId, eventType, status, amount, metadata, timestamp
            );

        } catch (SignatureVerificationException e) {
            // Falha criptográfica: A requisição foi alterada no meio do caminho ou forjada
            throw new SecurityException("Assinatura do Stripe inválida. Tentativa de fraude bloqueada.", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao interpretar o payload do webhook.", e);
        }
    }

    @Override
    public String createPaymentLink(UUID appointmentId, BigDecimal amount, String destinationAccountId,
            BigDecimal feePercentage) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://app.stylo.com/agendamentos/sucesso")
                    .setCancelUrl("https://app.stylo.com/agendamentos/falha")
                    .putMetadata("appointmentId", appointmentId.toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("brl")
                                    .setUnitAmount(amountInCents)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Serviço Agendado - Stylo")
                                            .build())
                                    .build())
                            .build());

            // ✨ SPLIT DE PAGAMENTO (Marketplace Logic)
            if (destinationAccountId != null && !destinationAccountId.isBlank()) {
                long appFeeInCents = amount.multiply(feePercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .longValue();

                paramsBuilder.setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .setApplicationFeeAmount(appFeeInCents)
                                .setTransferData(SessionCreateParams.PaymentIntentData.TransferData.builder()
                                        .setDestination(destinationAccountId)
                                        .build())
                                .build());
            }

            Session session = Session.create(paramsBuilder.build());
            return session.getUrl();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar checkout Stripe: " + e.getMessage());
        }
    }

    @Override
    public ConnectOnboardingResult createConnectAccountLink(UUID providerId, String email) {
        try {
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry("BR")
                    .setEmail(email)
                    .setCapabilities(AccountCreateParams.Capabilities.builder()
                            .setTransfers(
                                    AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                            .build())
                    .setMetadata(Map.of("providerId", providerId.toString()))
                    .build();

            Account account = Account.create(params);

            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(account.getId())
                    .setRefreshUrl("https://app.stylo.com/settings/payments/refresh")
                    .setReturnUrl("https://app.stylo.com/settings/payments/success")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            return new ConnectOnboardingResult(account.getId(), AccountLink.create(linkParams).getUrl());
        } catch (Exception e) {
            throw new RuntimeException("Erro no onboarding Stripe Connect: " + e.getMessage());
        }
    }

    @Override
    public boolean processDirectPayment(String customerId, BigDecimal amount, String paymentMethodId) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency("brl")
                    .setCustomer(customerId)
                    .setPaymentMethod(paymentMethodId)
                    .setConfirm(true)
                    .setOffSession(true)
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return "succeeded".equals(intent.getStatus());
        } catch (Exception e) {
            log.error("Erro no pagamento direto: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void refundPayment(String externalPaymentId, BigDecimal amount) {
        try {
            RefundCreateParams.Builder params = RefundCreateParams.builder().setPaymentIntent(externalPaymentId);
            if (amount != null) {
                params.setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue());
            }
            Refund.create(params.build());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar estorno Stripe: " + e.getMessage());
        }
    }

    @Override
    public void executeTransfer(String sourceTransactionId, String destinationAccountId, BigDecimal amount) {
        try {
            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency("brl")
                    .setDestination(destinationAccountId)
                    .setSourceTransaction(sourceTransactionId)
                    .build();

            Transfer.create(params);
        } catch (Exception e) {
            throw new RuntimeException("Erro na transferência manual Stripe: " + e.getMessage());
        }
    }

    @Override
    public boolean isAccountFullyOnboarded(String stripeAccountId) {
        if (stripeAccountId == null)
            return false;
        try {
            Account account = Account.retrieve(stripeAccountId);
            return Boolean.TRUE.equals(account.getChargesEnabled()) && Boolean.TRUE.equals(account.getPayoutsEnabled());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String continueOnboarding(String stripeAccountId) {
        try {
            AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                    .setAccount(stripeAccountId)
                    .setRefreshUrl("https://app.stylo.com/settings/payments")
                    .setReturnUrl("https://app.stylo.com/settings/payments")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();
            return AccountLink.create(params).getUrl();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar link de continuação: " + e.getMessage());
        }
    }

    @Override
    public String createLoginLink(String stripeAccountId) {
        try {
            return LoginLink.createOnAccount(stripeAccountId, (Map<String, Object>) null, null).getUrl();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar Login Link Stripe: " + e.getMessage());
        }
    }
}