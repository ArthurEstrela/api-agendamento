package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.usecases.dto.ExternalEvent;
import java.util.List;

public interface ICalendarProvider {
    void createEvent(Appointment appointment); 
    List<ExternalEvent> fetchRecentEvents(String professionalId);
    void watchCalendar(String professionalId, String webhookUrl);
}