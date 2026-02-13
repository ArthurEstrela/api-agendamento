package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ProcessSubscriptionStatusUseCase {

    private final IServiceProviderRepository providerRepository;
    private final INotificationProvider notificationProvider;

    @Transactional
    public void execute() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Processa Trials Expirados
        List<ServiceProvider> expiredTrials = providerRepository.findExpiredTrials(now);
        expiredTrials.forEach(provider -> {
            provider.updateSubscription("EXPIRED");
            providerRepository.save(provider);
            log.info("Trial expirado para o estabelecimento: {}", provider.getBusinessName());
            sendNotification(provider, "Seu período de teste acabou. Assine agora para continuar agendando!");
        });

        // 2. Processa Grace Periods Expirados (Tolerância esgotada)
        List<ServiceProvider> expiredGracePeriods = providerRepository.findExpiredGracePeriods(now);
        expiredGracePeriods.forEach(provider -> {
            provider.updateSubscription("EXPIRED");
            providerRepository.save(provider);
            log.error("Grace Period encerrado para {}. Acesso bloqueado por falta de pagamento.", provider.getBusinessName());
            sendNotification(provider, "Sua assinatura expirou após o período de carência. Regularize seu pagamento.");
        });

        // 3. Alerta de Expiração Iminente (Opcional - Toque de mestre)
        // Avisa quem expira em exatamente 3 dias
        List<ServiceProvider> upcomingExpirations = providerRepository.findUpcomingExpirations(now.plusDays(3));
        upcomingExpirations.forEach(provider -> {
            sendNotification(provider, "Sua assinatura vence em 3 dias. Evite bloqueios na sua agenda!");
        });
    }

    private void sendNotification(ServiceProvider provider, String message) {
        try {
            notificationProvider.sendNotification(
                provider.getId(), 
                "💳 Status da Assinatura", 
                message, 
                "/dashboard/subscription"
            );
        } catch (Exception e) {
            log.error("Erro ao enviar notificação de assinatura para {}: {}", provider.getId(), e.getMessage());
        }
    }
}