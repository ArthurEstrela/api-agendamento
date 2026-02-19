package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.ServiceProvider.SubscriptionStatus;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CheckExpiredTrialsUseCase {

    private final IServiceProviderRepository providerRepository;
    private final IUserRepository userRepository;
    private final INotificationProvider notificationProvider;

    @Transactional
    public void execute() {
        log.info("🔍 Iniciando verificação de estabelecimentos com Trial expirado...");

        // Busca estabelecimentos onde trialEndsAt < agora e status ainda é TRIAL
        List<ServiceProvider> expiredProviders = providerRepository.findExpiredTrials(LocalDateTime.now());

        for (ServiceProvider provider : expiredProviders) {
            try {
                // 1. Atualiza Status via Domínio (Usa Enum Seguro)
                provider.updateSubscriptionStatus(SubscriptionStatus.EXPIRED);
                providerRepository.save(provider);

                // 2. Notifica o Proprietário (Auditoria via Email para localizar o User)
                userRepository.findByEmail(provider.getOwnerEmail()).ifPresent(owner -> {
                    String title = "Seu período de teste acabou 🔒";
                    String body = String.format("O sistema de agendamentos do %s foi pausado. Assine um plano para continuar operando.", 
                            provider.getBusinessName());
                    
                    notificationProvider.sendPushNotification(owner.getId(), title, body, "/plans");
                });
                
                log.info("Trial expirado processado: {} (ID: {})", provider.getBusinessName(), provider.getId());
                
            } catch (Exception e) {
                log.error("Erro ao processar expiração do establishment {}: {}", provider.getId(), e.getMessage());
            }
        }
    }
}