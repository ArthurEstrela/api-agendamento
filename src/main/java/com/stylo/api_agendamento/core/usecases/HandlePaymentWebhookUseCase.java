package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HandlePaymentWebhookUseCase {

    private final IServiceProviderRepository serviceProviderRepository;

    public void execute(PaymentWebhookInput input) {
        // 1. Busca o estabelecimento vinculado ao evento de pagamento
        ServiceProvider provider = serviceProviderRepository.findById(input.providerId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado para o ID: " + input.providerId()));

        // 2. Lógica de Negócio: Trata o tipo de evento do Stripe
        if ("checkout.session.completed".equals(input.eventType()) || "invoice.paid".equals(input.eventType())) {
            
            // Se o pagamento teve sucesso, ativa a assinatura
            if ("succeeded".equalsIgnoreCase(input.status()) || "paid".equalsIgnoreCase(input.status())) {
                provider.updateSubscription("ACTIVE");
            }
        } 
        
        else if ("invoice.payment_failed".equals(input.eventType())) {
            // Se o pagamento falhou, podemos marcar como EXPIRED ou lançar um aviso
            provider.updateSubscription("EXPIRED");
        }

        // 3. Persistência da mudança de estado no domínio
        serviceProviderRepository.save(provider);
        
        // DICA: Aqui você poderia disparar um evento para o INotificationProvider 
        // avisando o dono do salão: "Parabéns! Sua assinatura Stylo está ativa."
    }
}