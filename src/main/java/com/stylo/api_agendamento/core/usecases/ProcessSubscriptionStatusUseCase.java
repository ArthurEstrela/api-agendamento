package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.ServiceProvider.SubscriptionStatus;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ProcessSubscriptionStatusUseCase {

    private final IServiceProviderRepository providerRepository;
    private final INotificationProvider notificationProvider;

    @Transactional
    public void execute() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Processa Trials Expirados (Conversão de Free para Paid)
        List<ServiceProvider> expiredTrials = providerRepository.findExpiredTrials(now);
        expiredTrials.forEach(provider -> {
            provider.updateSubscriptionStatus(SubscriptionStatus.EXPIRED);
            providerRepository.save(provider);
            log.info("Trial expirado: {}", provider.getBusinessName());
            sendNotification(provider.getId(), "🔒 Período de teste encerrado. Assine para não perder seus agendamentos!");
        });

        // 2. Processa Grace Periods Expirados (Inadimplência definitiva)
        List<ServiceProvider> expiredGrace = providerRepository.findExpiredGracePeriods(now);
        expiredGrace.forEach(provider -> {
            provider.updateSubscriptionStatus(SubscriptionStatus.EXPIRED);
            providerRepository.save(provider);
            log.warn("Bloqueio por inadimplência: {}", provider.getBusinessName());
            sendNotification(provider.getId(), "❌ Sua assinatura foi suspensa por falta de pagamento.");
        });

        // 3. Toque de Mestre: Alerta Iminente (Retenção)
        // Busca quem expira em exatamente 3 dias para incentivar a renovação
        providerRepository.findUpcomingExpirations(now.plusDays(3))
            .forEach(provider -> sendNotification(provider.getId(), "⏳ Sua assinatura vence em 3 dias. Evite interrupções na sua agenda!"));
    }

    private void sendNotification(UUID providerId, String message) {
        try {
            notificationProvider.sendPushNotification(
                providerId, 
                "💳 Gestão de Assinatura", 
                message, 
                "/admin/billing"
            );
        } catch (Exception e) {
            log.error("Falha ao notificar provider {}: {}", providerId, e.getMessage());
        }
    }
}