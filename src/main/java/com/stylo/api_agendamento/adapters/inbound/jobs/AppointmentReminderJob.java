package com.stylo.api_agendamento.adapters.inbound.jobs;

import com.stylo.api_agendamento.core.usecases.SendRemindersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j // ✨ Adicionado para logs profissionais
@Component
@RequiredArgsConstructor
public class AppointmentReminderJob {

    private final SendRemindersUseCase sendRemindersUseCase;

    /**
     * Executa a verificação de lembretes.
     * fixedRateString permite que você configure o tempo via application.properties 
     * sem precisar mexer no código depois.
     */
    @Scheduled(fixedRateString = "${stylo.jobs.reminder-interval:60000}") 
    public void run() {
        log.info("⏰ [Job] Iniciando processamento de lembretes precisos...");
        
        long startTime = System.currentTimeMillis();

        try {
            // Executa o Use Case que busca agendamentos confirmados e dispara Push/Email
            sendRemindersUseCase.execute();
            
            long endTime = System.currentTimeMillis();
            log.info("✅ [Job] Lembretes processados com sucesso em {}ms.", (endTime - startTime));
            
        } catch (Exception e) {
            // 🔥 Crucial: Evita que uma falha em um agendamento pare o agendador do Spring
            log.error("❌ [Job] Erro crítico ao processar lembretes: {}", e.getMessage(), e);
        }
    }
}