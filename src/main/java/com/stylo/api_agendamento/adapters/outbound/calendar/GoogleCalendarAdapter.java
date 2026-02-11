// src/main/java/com/stylo/api_agendamento/adapters/outbound/calendar/GoogleCalendarAdapter.java
package com.stylo.api_agendamento.adapters.outbound.calendar;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.usecases.dto.ExternalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements ICalendarProvider {

    @Override
    public void createEvent(Appointment appointment) {
        try {
            Calendar service = getGoogleServiceForProfessional(appointment.getProfessionalId());
            if (service == null) return;

            Event event = new Event()
                .setSummary(appointment.getBusinessName() + " - " + appointment.getProfessionalName())
                .setDescription("Serviços: " + appointment.getServices().stream()
                    .map(s -> s.getName()).reduce((a, b) -> a + ", " + b).orElse(""))
                .setLocation("Agendado via Stylo App");

            // Formatação correta para RFC3339
            event.setStart(new EventDateTime().setDateTime(new DateTime(appointment.getStartTime().toString() + ":00Z")));
            event.setEnd(new EventDateTime().setDateTime(new DateTime(appointment.getEndTime().toString() + ":00Z")));

            service.events().insert("primary", event).execute();
        } catch (Exception e) {
            log.error("Erro ao criar evento no Google Calendar: {}", e.getMessage());
        }
    }

    @Override
    public List<ExternalEvent> fetchRecentEvents(String professionalId) {
        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);
            if (service == null) return Collections.emptyList();

            // Busca eventos modificados na última hora ou futuros
            DateTime now = new DateTime(System.currentTimeMillis());
            Events events = service.events().list("primary")
                .setTimeMin(now)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

            return events.getItems().stream()
                .map(this::toExternalEvent)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erro ao buscar eventos do Google: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void watchCalendar(String professionalId, String webhookUrl) {
        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);
            if (service == null) return;

            Channel channel = new Channel()
                .setId(UUID.randomUUID().toString()) // ID único para este canal de vigilância
                .setType("web_hook")
                .setAddress(webhookUrl);

            service.events().watch("primary", channel).execute();
            log.info("Webhook do Google Calendar ativado para o profissional: {}", professionalId);

        } catch (Exception e) {
            log.error("Erro ao configurar watch no Google Calendar: {}", e.getMessage());
        }
    }

    // Conversor de Evento do Google para o seu DTO de Use Case
    private ExternalEvent toExternalEvent(Event googleEvent) {
        LocalDateTime start = LocalDateTime.ofInstant(
            java.util.Date.from(java.time.Instant.ofEpochMilli(googleEvent.getStart().getDateTime().getValue())).toInstant(), 
            ZoneId.systemDefault()
        );
        LocalDateTime end = LocalDateTime.ofInstant(
            java.util.Date.from(java.time.Instant.ofEpochMilli(googleEvent.getEnd().getDateTime().getValue())).toInstant(), 
            ZoneId.systemDefault()
        );

        return new ExternalEvent(
            googleEvent.getId(),
            googleEvent.getSummary(),
            start,
            end
        );
    }

    private Calendar getGoogleServiceForProfessional(String professionalId) {
        // TODO: Implementar a recuperação do Token OAuth2 do banco de dados
        // e instanciar o objeto 'com.google.api.services.calendar.Calendar'
        return null; 
    }
}