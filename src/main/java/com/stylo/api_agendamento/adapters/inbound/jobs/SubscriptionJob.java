package com.stylo.api_agendamento.adapters.inbound.jobs;

import com.stylo.api_agendamento.core.usecases.ProcessSubscriptionStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionJob {

    private final ProcessSubscriptionStatusUseCase processSubscriptionStatusUseCase;

    /**
     * Roda diariamente às 00:00 (Fuso de Brasília)
     * Gerencia trials, grace periods e notificações de renovação.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Sao_Paulo")
    public void run() {
        log.info("Iniciando processamento diário de assinaturas...");
        try {
            processSubscriptionStatusUseCase.execute();
            log.info("Processamento de assinaturas concluído com sucesso.");
        } catch (Exception e) {
            log.error("Falha crítica no SubscriptionJob: {}", e.getMessage());
        }
    }
}