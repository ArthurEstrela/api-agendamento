package com.stylo.api_agendamento.core.usecases;

import java.util.List;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.usecases.dto.ExternalEvent;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SyncExternalCalendarUseCase {
    private final ICalendarProvider calendarProvider;
    private final BlockProfessionalTimeUseCase blockTimeUseCase;
    private final IProfessionalRepository professionalRepository;

    public void execute(String professionalId) {
        // 1. Busca eventos recentes do Google Calendar
        List<ExternalEvent> externalEvents = calendarProvider.fetchRecentEvents(professionalId);

        // 2. Para cada evento externo, cria um bloqueio no Stylo
        for (ExternalEvent event : externalEvents) {
            var blockInput = new BlockProfessionalTimeUseCase.BlockTimeInput(
                professionalId,
                event.startTime(),
                event.endTime(),
                "Sincronizado via Google: " + event.summary()
            );
            
            try {
                blockTimeUseCase.execute(blockInput);
            } catch (BusinessException e) {
                // Se já houver um bloqueio ou conflito, apenas ignora
            }
        }
    }
}