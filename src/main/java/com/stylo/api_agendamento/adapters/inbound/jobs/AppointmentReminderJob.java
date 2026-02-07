package com.stylo.api_agendamento.adapters.inbound.jobs;

import com.stylo.api_agendamento.core.usecases.SendRemindersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentReminderJob {

    private final SendRemindersUseCase sendRemindersUseCase;

    // Roda a cada 1 minuto (60000ms)
    @Scheduled(fixedRate = 60000)
    public void run() {
        sendRemindersUseCase.execute();
    }
}