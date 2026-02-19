package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.usecases.dto.ExternalEvent;

import java.util.List;
import java.util.UUID;

public interface ICalendarProvider {

    /**
     * Cria um evento no calendário externo (Google/Outlook).
     * @return O ID do evento gerado externamente (String).
     */
    String createEvent(Appointment appointment);

    /**
     * Atualiza um evento existente (ex: reagendamento).
     */
    void updateEvent(Appointment appointment, String externalEventId);

    /**
     * Busca eventos recentes do calendário externo para sincronização reversa (Bloqueio de agenda).
     */
    List<ExternalEvent> fetchRecentEvents(UUID professionalId);

    /**
     * Configura o webhook para escutar mudanças no calendário do profissional.
     */
    void watchCalendar(UUID professionalId, String webhookUrl);

    /**
     * Remove o evento do calendário externo.
     */
    void deleteEvent(UUID professionalId, String externalEventId);
}