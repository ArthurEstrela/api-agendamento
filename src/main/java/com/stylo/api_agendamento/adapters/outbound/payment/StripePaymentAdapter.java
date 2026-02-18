package com.stylo.api_agendamento.adapters.outbound.payment;

import com.stripe.Stripe;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;

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

            // Extrai a Session se for checkout, ou PaymentIntent se for direto
            if (event.getDataObjectDeserializer().getObject().isPresent()) {
                Object stripeObject = event.getDataObjectDeserializer().getObject().get();

                if (stripeObject instanceof Session session) {
                    return new PaymentWebhookInput(
                            event.getId(),
                            session.getPaymentIntent(),
                            event.getType(),
                            session.getPaymentStatus(), // "paid", "unpaid"
                            BigDecimal.valueOf(session.getAmountTotal() / 100.0),
                            session.getMetadata(),
                            Instant.ofEpochSecond(event.getCreated()));
                }
            }
            // Se não for Session, retornamos null ou lançamos erro dependendo da estratégia
            // Aqui permitimos passar sem payload rico se for apenas um evento de status
            // simples
            throw new SecurityException("Evento Stripe ignorado ou payload vazio.");

        } catch (Exception e) {
            throw new SecurityException("Webhook inválido ou erro de parse: " + e.getMessage());
        }
    }

    @Override
    public String generatePaymentLink(String appointmentId, BigDecimal amount, String destinationAccountId,
            BigDecimal feePercentage) {
        try {
            long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

            SessionCreateParams.Builder paramsBuilder = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("https://seusite.com/agendamentos/sucesso")
                    .setCancelUrl("https://seusite.com/agendamentos/falha")
                    .putMetadata("context", "APPOINTMENT")
                    .putMetadata("appointmentId", appointmentId)
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

            // ✨ A MÁGICA DO SPLIT AUTOMÁTICO
            // Se tiver um ID de barbeiro (destinationAccountId), o Stripe divide o dinheiro
            // agora.
            if (destinationAccountId != null && !destinationAccountId.isBlank()) {

                // Calcula sua comissão em centavos
                long appFee = amount.multiply(feePercentage)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .longValue();

                paramsBuilder.setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .setApplicationFeeAmount(appFee) // Sua parte (fica na sua conta)
                                .setTransferData(
                                        SessionCreateParams.PaymentIntentData.TransferData.builder()
                                                .setDestination(destinationAccountId) // O resto vai pro barbeiro
                                                .build())
                                .build());
            }

            Session session = Session.create(paramsBuilder.build());
            return session.getUrl();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar link Stripe Checkout: " + e.getMessage());
        }
    }

    @Override
    public ConnectOnboardingResult createConnectAccountLink(String providerId, String email) {
        try {
            // 1. Criar a conta (Express é ideal para SaaS no Brasil)
            AccountCreateParams params = AccountCreateParams.builder()
                    .setType(AccountCreateParams.Type.EXPRESS)
                    .setCountry("BR")
                    .setEmail(email)
                    .setCapabilities(AccountCreateParams.Capabilities.builder()
                            .setTransfers(
                                    AccountCreateParams.Capabilities.Transfers.builder().setRequested(true).build())
                            .build())
                    .setMetadata(Map.of("providerId", providerId))
                    .build();

            Account account = Account.create(params);

            // 2. Gerar o Link
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(account.getId())
                    .setRefreshUrl("https://app.stylo.com/settings/payments/refresh")
                    .setReturnUrl("https://app.stylo.com/settings/payments/success")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();

            AccountLink accountLink = AccountLink.create(linkParams);

            // ✨ CORREÇÃO: Retorna o ID e a URL para o UseCase salvar no banco
            return new ConnectOnboardingResult(account.getId(), accountLink.getUrl());

        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar conta Stripe Connect: " + e.getMessage());
        }
    }

    @Override
    public boolean processPayment(String customerId, BigDecimal amount, String paymentMethodId) {
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
            // Logar erro real é importante
            System.err.println("Erro Stripe Payment: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void refund(String externalPaymentId, BigDecimal amount) {
        try {
            RefundCreateParams.Builder params = RefundCreateParams.builder()
                    .setPaymentIntent(externalPaymentId);

            if (amount != null) {
                params.setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue());
            }

            Refund.create(params.build());
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar estorno: " + e.getMessage());
        }
    }

    @Override
    public void executeSplit(String paymentId, String targetAccountId, BigDecimal profAmount,
            BigDecimal providerAmount) {
        // Método de fallback para transferências manuais (se não usar o split
        // automático no checkout)
        try {
            TransferCreateParams params = TransferCreateParams.builder()
                    .setAmount(profAmount.multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency("brl")
                    .setDestination(targetAccountId)
                    .setSourceTransaction(paymentId) // Importante: vincula à cobrança original
                    .build();

            Transfer.create(params);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao executar Transferência Manual: " + e.getMessage());
        }
    }

    @Override
    public boolean isAccountFullyOnboarded(String stripeAccountId) {
        if (stripeAccountId == null)
            return false;
        try {
            Account account = Account.retrieve(stripeAccountId);
            return Boolean.TRUE.equals(account.getChargesEnabled())
                    && Boolean.TRUE.equals(account.getPayoutsEnabled());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String continueOnboarding(String stripeAccountId) {
        try {
            AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                    .setAccount(stripeAccountId)
                    .setRefreshUrl("https://seu-app.com/painel/financeiro")
                    .setReturnUrl("https://seu-app.com/painel/financeiro")
                    .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                    .build();
            return AccountLink.create(linkParams).getUrl();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar link de continuação: " + e.getMessage());
        }
    }

    @Override
    public String createLoginLink(String stripeAccountId) {
        try {
            // Gera link mágico para o dashboard financeiro do Stripe (sem senha)
            return com.stripe.model.LoginLink.createOnAccount(
                    stripeAccountId,
                    (Map<String, Object>) null,
                    null).getUrl();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar link de login Stripe: " + e.getMessage());
        }
    }
}