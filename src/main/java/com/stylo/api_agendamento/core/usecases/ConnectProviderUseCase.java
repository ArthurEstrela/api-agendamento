package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ConnectProviderUseCase {

    private final IServiceProviderRepository serviceProviderRepository;
    private final IPaymentProvider paymentProvider;

    /**
     * Gerencia o ciclo de vida da conta Stripe Connect do prestador.
     * @return URL para redirecionamento (Onboarding ou Dashboard)
     */
    public String execute(String providerId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Prestador não encontrado."));

        // CENÁRIO A: Prestador nunca iniciou o processo
        if (provider.getStripeAccountId() == null || provider.getStripeAccountId().isBlank()) {
            log.info("Iniciando onboarding Stripe para provedor: {}", provider.getBusinessName());
            
            var result = paymentProvider.createConnectAccountLink(provider.getId(), provider.getOwnerEmail());
            
            // Salva o ID da conta IMEDIATAMENTE para vincular o prestador à conta Stripe
            provider.setStripeAccountId(result.accountId());
            serviceProviderRepository.save(provider); // Atualiza no banco

            return result.onboardingUrl();
        }

        // CENÁRIO B: Já tem conta, vamos verificar o status
        String accountId = provider.getStripeAccountId();
        boolean isOnboardingComplete = paymentProvider.isAccountFullyOnboarded(accountId);

        // Se o status mudou para completo e não sabíamos, atualizamos
        if (isOnboardingComplete && !Boolean.TRUE.equals(provider.getOnlinePaymentsEnabled())) {
            provider.setOnlinePaymentsEnabled(true);
            serviceProviderRepository.save(provider);
            log.info("Onboarding concluído. Pagamentos online ativados para: {}", provider.getBusinessName());
        }

        // Se está completo, geramos link de Login para o Dashboard (ver saldo/saques)
        if (isOnboardingComplete) {
            return paymentProvider.createLoginLink(accountId);
        }

        // CENÁRIO C: Tem conta mas não terminou o cadastro
        log.info("Retomando onboarding pendente para: {}", provider.getBusinessName());
        return paymentProvider.continueOnboarding(accountId);
    }
}