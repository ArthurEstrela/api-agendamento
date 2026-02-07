package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IPaymentProvider; // Adicionado
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HandlePaymentWebhookUseCase {

    private final IServiceProviderRepository serviceProviderRepository;
    private final IPaymentProvider paymentProvider; // Agora casa com o Bean

    public void execute(PaymentWebhookInput input) {
        // 1. Opcional: Validar a assinatura do Webhook usando o Provider (Segurança)
        // paymentProvider.verifyWebhookSignature(input.rawPayload(), input.signature());

        // 2. Busca o estabelecimento
        ServiceProvider provider = serviceProviderRepository.findById(input.providerId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 3. Lógica de Negócio (Stripe/MercadoPago)
        if ("checkout.session.completed".equals(input.eventType()) || "invoice.paid".equals(input.eventType())) {
            if ("succeeded".equalsIgnoreCase(input.status()) || "paid".equalsIgnoreCase(input.status())) {
                provider.updateSubscription("ACTIVE");
            }
        } 
        else if ("invoice.payment_failed".equals(input.eventType())) {
            provider.updateSubscription("EXPIRED");
        }

        // 4. Salva a mudança
        serviceProviderRepository.save(provider);
    }
}