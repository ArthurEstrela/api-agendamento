package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.usecases.dto.ExternalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SyncExternalCalendarUseCase {
    
    private final ICalendarProvider calendarProvider;
    private final BlockProfessionalTimeUseCase blockTimeUseCase;
    private final IAppointmentRepository appointmentRepository;

    public void execute(UUID professionalId) {
        log.info("🔄 Iniciando sincronização de calendário externo para o profissional: {}", professionalId);

        // 1. Busca eventos recentes (Google/Outlook)
        List<ExternalEvent> externalEvents = calendarProvider.fetchRecentEvents(professionalId);

        for (ExternalEvent event : externalEvents) {
            // 2. Idempotência: Verifica se este evento externo já foi sincronizado
            if (appointmentRepository.existsByExternalEventId(event.externalId())) {
                continue;
            }

            try {
                // 3. Cria o bloqueio no Stylo para "travar" a agenda
                var blockInput = new BlockProfessionalTimeUseCase.Input(
                        professionalId,
                        event.startTime(),
                        event.endTime(),
                        "Sincronizado: " + event.summary()
                );
                
                blockTimeUseCase.execute(blockInput);
                log.info("✅ Evento externo '{}' sincronizado como bloqueio.", event.summary());

            } catch (BusinessException e) {
                log.warn("⚠️ Conflito ao sincronizar evento '{}': {}", event.summary(), e.getMessage());
            } catch (Exception e) {
                log.error("❌ Erro inesperado na sincronização do evento {}: {}", event.externalId(), e.getMessage());
            }
        }
    }
}