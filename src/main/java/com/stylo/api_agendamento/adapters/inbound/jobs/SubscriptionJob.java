package com.stylo.api_agendamento.adapters.inbound.jobs;

import com.stylo.api_agendamento.core.usecases.CheckExpiredTrialsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionJob {

    private final CheckExpiredTrialsUseCase checkExpiredTrialsUseCase;

    // Roda todo dia à meia-noite (00:00)
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Sao_Paulo")
    public void run() {
        checkExpiredTrialsUseCase.execute();
    }
}