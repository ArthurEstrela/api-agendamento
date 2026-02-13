package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CheckExpiredTrialsUseCase {

    private final IServiceProviderRepository providerRepository;
    private final IUserRepository userRepository;
    private final INotificationProvider notificationProvider;

    public void execute() {
        log.info("🔍 Verificando Service Providers com trial expirado...");

        List<ServiceProvider> expiredProviders = providerRepository.findExpiredTrials(LocalDateTime.now());

        for (ServiceProvider provider : expiredProviders) {
            try {
                // 1. Atualiza Status usando o método do seu domínio
                // Correção: setSubscriptionStatus -> updateSubscription
                provider.updateSubscription("EXPIRED"); 
                providerRepository.save(provider);

                // 2. Notifica o Dono
                userRepository.findByEmail(provider.getOwnerEmail()).ifPresent(owner -> {
                    String title = "Seu período de teste acabou 🔒";
                    
                    // Correção: getName -> getBusinessName
                    String body = "O agendamento do " + provider.getBusinessName() + " foi pausado. Assine agora para continuar.";
                    
                    notificationProvider.sendNotification(owner.getId(), title, body, "/plans");
                });
                
                // Correção: getName -> getBusinessName
                log.info("Trial expirado para o estabelecimento: {}", provider.getBusinessName());
                
            } catch (Exception e) {
                log.error("Erro ao processar expiração do provider {}: {}", provider.getId(), e.getMessage());
            }
        }
    }
}