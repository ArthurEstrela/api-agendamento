package com.stylo.api_agendamento.adapters.inbound.listeners;

import com.stylo.api_agendamento.core.domain.events.ProductLowStockEvent;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockAlertListener {

    private final INotificationProvider notificationProvider;
    private final IServiceProviderRepository providerRepository; // ✨ Localiza o dono pelo Provider
    private final IUserRepository userRepository; // ✨ Localiza o usuário para o Push

    @Async
    @EventListener
    public void handleLowStock(ProductLowStockEvent event) {
        log.warn("ALERTA DE ESTOQUE: Produto '{}' (ID: {}) atingiu nível crítico: {} (Mínimo: {})", 
                 event.productName(), event.productId(), event.currentStock(), event.minThreshold());

        // 1. Localiza o estabelecimento para identificar o email do proprietário
        providerRepository.findById(event.providerId()).ifPresentOrElse(provider -> {
            
            // 2. Busca a entidade User do proprietário para obter o ID de notificação push
            userRepository.findByEmail(provider.getOwnerEmail()).ifPresentOrElse(owner -> {
                
                String title = "📦 Estoque Crítico!";
                String body = String.format("Atenção! O produto '%s' está acabando. Restam apenas %d unidades no estabelecimento %s (Limite mínimo: %d).",
                        event.productName(), event.currentStock(), provider.getBusinessName(), event.minThreshold());

                // 3. Envia Notificação Push para o App do Gestor
                notificationProvider.sendPushNotification(
                        owner.getId(),
                        title,
                        body,
                        "/inventory" // Deep link para a tela de estoque no App
                );

                // 4. Envia Alerta de Sistema (E-mail) como redundância de segurança
                notificationProvider.sendSystemAlert(
                        owner.getEmail(),
                        "Alerta de Estoque: " + event.productName(),
                        body
                );

                log.info("Alertas de estoque baixo enviados para o gestor: {} ({})", 
                        owner.getName(), provider.getBusinessName());

            }, () -> log.error("Falha ao alertar estoque: Usuário proprietário não encontrado para o email {}", provider.getOwnerEmail()));
            
        }, () -> log.error("Falha ao alertar estoque: Estabelecimento não encontrado para o ID {}", event.providerId()));
    }
}