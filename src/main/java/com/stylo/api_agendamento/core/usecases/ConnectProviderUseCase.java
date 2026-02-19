package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ConnectProviderUseCase {

    private final IServiceProviderRepository serviceProviderRepository;
    private final IPaymentProvider paymentProvider;

    /**
     * Gerencia a conexão com o Stripe Connect.
     * @return URL para redirecionamento (Onboarding, Retomada ou Dashboard Financeiro).
     */
    @Transactional
    public String execute(UUID providerId) {
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // CENÁRIO A: Prestador nunca iniciou o processo de conexão
        if (provider.getStripeAccountId() == null || provider.getStripeAccountId().isBlank()) {
            log.info("Iniciando fluxo de onboarding Stripe para: {}", provider.getBusinessName());
            
            // Cria a conta na plataforma e gera o link inicial
            var result = paymentProvider.createConnectAccountLink(provider.getId(), provider.getOwnerEmail());
            
            // Vincula o ID da conta imediatamente (Método rico do domínio)
            provider.enableOnlinePayments(result.accountId());
            serviceProviderRepository.save(provider);

            return result.onboardingUrl();
        }

        String accountId = provider.getStripeAccountId();
        
        // CENÁRIO B: Verifica se o cadastro foi finalizado junto ao gateway
        boolean isOnboardingComplete = paymentProvider.isAccountFullyOnboarded(accountId);

        if (isOnboardingComplete) {
            // Se o status mudou para completo no gateway mas não no nosso banco
            if (!provider.isOnlinePaymentsEnabled()) {
                provider.enableOnlinePayments(accountId);
                serviceProviderRepository.save(provider);
                log.info("Onboarding finalizado. Pagamentos ativados para: {}", provider.getBusinessName());
            }
            
            // Retorna link direto para o Dashboard de saques e saldo
            return paymentProvider.createLoginLink(accountId);
        }

        // CENÁRIO C: Tem conta criada mas o cadastro está incompleto/pendente
        log.info("Retomando onboarding pendente para o provider: {}", provider.getBusinessName());
        return paymentProvider.continueOnboarding(accountId);
    }
}