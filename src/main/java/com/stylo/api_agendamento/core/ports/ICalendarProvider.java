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
     * Atualiza um evento existente.
     * ✨ CORREÇÃO: O externalEventId é extraído diretamente do objeto Appointment.
     */
    void updateEvent(Appointment appointment);

    /**
     * Remove o evento do calendário externo.
     * ✨ CORREÇÃO: Ordem dos parâmetros ajustada para (externalEventId, professionalId) 
     * conforme a necessidade do fluxo de sincronização.
     */
    void deleteEvent(String externalEventId, UUID professionalId);

    /**
     * Busca eventos recentes do calendário externo para sincronização reversa (Bloqueio de agenda).
     */
    List<ExternalEvent> fetchRecentEvents(UUID professionalId);

    /**
     * Configura o webhook para escutar mudanças no calendário do profissional.
     */
    void watchCalendar(UUID professionalId, String webhookUrl);

    /**
     * ✨ COMPLEMENTO: Cancela o monitoramento do calendário.
     * Essencial para processos de limpeza ou quando um profissional desvincula sua conta.
     */
    void stopWatchingCalendar(UUID professionalId);
}