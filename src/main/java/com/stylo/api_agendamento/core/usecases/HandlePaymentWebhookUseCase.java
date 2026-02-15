package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class HandlePaymentWebhookUseCase {

    private final IServiceProviderRepository serviceProviderRepository;
    private final IPaymentProvider paymentProvider;

    public void execute(PaymentWebhookInput input) {
        // 1. Validação de Segurança (Opcional, mas recomendado para produção)
        // paymentProvider.verifyWebhookSignature(input.rawPayload(), input.signature());

        // 2. Busca o estabelecimento
        ServiceProvider provider = serviceProviderRepository.findById(input.providerId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        log.info("Processando evento de pagamento '{}' para o provedor: {}", input.eventType(), provider.getBusinessName());

        // 3. Lógica de Negócio (Orquestração de Assinatura)
        switch (input.eventType()) {
            case "checkout.session.completed", "invoice.paid" -> {
                if ("succeeded".equalsIgnoreCase(input.status()) || "paid".equalsIgnoreCase(input.status())) {
                    provider.updateSubscription("ACTIVE");
                    log.info("Assinatura do provedor {} reativada com sucesso.", provider.getId());
                }
            }
            case "invoice.payment_failed" -> {
                // ✨ A Mágica do Grace Period:
                // Se o estabelecimento não estiver cancelado ou já expirado, damos 3 dias de lambuja.
                if (provider.isSubscriptionActive() && !"GRACE_PERIOD".equals(provider.getSubscriptionStatus())) {
                    provider.startGracePeriod(3);
                    log.warn("Falha no pagamento de {}. Iniciando Grace Period de 3 dias.", provider.getBusinessName());
                } else {
                    // Se já estava no Grace Period e falhou de novo, ou já estava irregular:
                    provider.updateSubscription("EXPIRED");
                    log.error("Assinatura de {} expirada após falha recorrente ou fim da carência.", provider.getId());
                }
            }
            default -> log.debug("Evento de webhook ignorado: {}", input.eventType());
        }

        // 4. Persiste a mudança
        serviceProviderRepository.save(provider);
    }
}