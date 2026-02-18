package com.stylo.api_agendamento.adapters.inbound.listeners;

import com.stylo.api_agendamento.core.domain.events.ProductLowStockEvent;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository; // Para achar o dono
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
    // private final IServiceProviderRepository providerRepository; (Opcional, se precisar achar o Admin ID)

    @Async
    @EventListener
    public void handleLowStock(ProductLowStockEvent event) {
        log.warn("ALERTA DE ESTOQUE: Produto '{}' atingiu nível crítico: {} (Mínimo: {})", 
                 event.productName(), event.currentStock(), event.minThreshold());

        // TODO: Buscar o ID do gestor do ProviderId e enviar notificação
        // notificationProvider.sendNotification(managerId, "Estoque Baixo 📦", ...);
    }
}