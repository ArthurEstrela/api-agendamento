package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.usecases.dto.ExternalEvent;
import java.util.List;

public interface ICalendarProvider {
    String createEvent(Appointment appointment);

    List<ExternalEvent> fetchRecentEvents(String professionalId);

    void watchCalendar(String professionalId, String webhookUrl);

    void deleteEvent(String professionalId, String externalEventId);
}