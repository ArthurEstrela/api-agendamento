package com.stylo.api_agendamento.adapters.outbound.payment;

import com.stripe.Stripe;
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
    public PaymentWebhookInput validateAndParseWebhook(String rawPayload, String signatureHeader) {
        try {
            Event event = Webhook.constructEvent(rawPayload, signatureHeader, webhookSecret);

            if (event.getDataObjectDeserializer().getObject().isPresent()) {
                Object stripeObject = event.getDataObjectDeserializer().getObject().get();

                if (stripeObject instanceof Session session) {
                    return new PaymentWebhookInput(
                            event.getId(),
                            session.getPaymentIntent(),
                            event.getType(),
                            session.getPaymentStatus(),
                            BigDecimal.valueOf(session.getAmountTotal() / 100.0),
                            session.getMetadata(),
                            Instant.ofEpochSecond(event.getCreated()));
                }
            }
            throw new SecurityException("Evento Stripe sem payload de Sessão válido.");
        } catch (Exception e) {
            log.error("Erro na validação do Webhook Stripe: {}", e.getMessage());
            throw new SecurityException("Webhook inválido: " + e.getMessage());
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