package com.stylo.api_agendamento.adapters.inbound.jobs;

import com.stylo.api_agendamento.core.usecases.SendPendingRemindersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderJob {

    // ✨ Atualizado para o Use Case de lembretes pendentes
    private final SendPendingRemindersUseCase sendRemindersUseCase;

    /**
     * Executa a verificação de lembretes.
     * * MELHORIA: Trocamos fixedRate por fixedDelay. 
     * O fixedDelay garante que a próxima execução só comece APÓS 
     * o término da anterior, evitando sobreposição caso o servidor 
     * de e-mail/push esteja lento.
     */
    @Scheduled(
        fixedDelayString = "${stylo.jobs.reminder-interval:60000}", 
        initialDelay = 10000 // Aguarda 10s após o boot para a primeira execução
    )
    public void run() {
        log.info("⏰ [Job Reminders] Iniciando verificação de agendamentos próximos...");
        
        long start = System.currentTimeMillis();

        try {
            // Executa a lógica de domínio para buscar, disparar e marcar lembretes
            sendRemindersUseCase.execute();
            
            long duration = System.currentTimeMillis() - start;
            log.info("✅ [Job Reminders] Lembretes processados com sucesso em {}ms.", duration);
            
        } catch (Exception e) {
            // Log detalhado para rastrear falhas sem interromper o agendador do Spring
            log.error("❌ [Job Reminders] Falha crítica ao processar ciclo de lembretes: {}", e.getMessage(), e);
        }
    }
}