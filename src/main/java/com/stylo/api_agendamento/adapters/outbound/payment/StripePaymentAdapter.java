package com.stylo.api_agendamento.adapters.outbound.payment;

import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;


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
            
            // Para Checkout Sessions (Pagamentos de agendamento ou assinatura)
            if (event.getDataObjectDeserializer().getObject().isPresent() && 
                event.getDataObjectDeserializer().getObject().get() instanceof Session session) {
                
                return new PaymentWebhookInput(
                    event.getId(),
                    session.getPaymentIntent(),
                    event.getType(),
                    session.getPaymentStatus(),
                    BigDecimal.valueOf(session.getAmountTotal() / 100.0),
                    session.getMetadata(),
                    Instant.ofEpochSecond(event.getCreated())
                );
            }
            throw new SecurityException("Evento não suportado para parsing automático.");
        } catch (Exception e) {
            throw new SecurityException("Falha na validação do Webhook Stripe: " + e.getMessage());
        }
    }

    @Override
    public String generatePaymentLink(String appointmentId, BigDecimal amount) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://seusite.com/sucesso")
                .setCancelUrl("https://seusite.com/cancelado")
                .putMetadata("context", "APPOINTMENT")
                .putMetadata("appointmentId", appointmentId)
                .addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency("brl")
                        .setUnitAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                            .setName("Serviço Stylo")
                            .build())
                        .build())
                    .build())
                .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar link Stripe: " + e.getMessage());
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
                .setOffSession(true) // Pagamento automático sem o cliente presente
                .build();

            PaymentIntent intent = PaymentIntent.create(params);
            return "succeeded".equals(intent.getStatus());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void refund(String externalPaymentId, BigDecimal amount) {
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(externalPaymentId)
                .setAmount(amount.multiply(BigDecimal.valueOf(100)).longValue())
                .build();

            Refund.create(params);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao processar estorno no Stripe: " + e.getMessage());
        }
    }

    @Override
    public void executeSplit(String paymentId, String targetAccountId, BigDecimal profAmount, BigDecimal providerAmount) {
        try {
            // No Stripe Connect, o Split geralmente é feito via Transferências após o pagamento principal cair na conta da plataforma
            TransferCreateParams params = TransferCreateParams.builder()
                .setAmount(profAmount.multiply(BigDecimal.valueOf(100)).longValue())
                .setCurrency("brl")
                .setDestination(targetAccountId) // ID da conta Stripe do profissional
                .setSourceTransaction(paymentId) // Vincula à transação original para conciliação
                .build();

            Transfer.create(params);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao executar Split (Transfer) no Stripe: " + e.getMessage());
        }
    }
}