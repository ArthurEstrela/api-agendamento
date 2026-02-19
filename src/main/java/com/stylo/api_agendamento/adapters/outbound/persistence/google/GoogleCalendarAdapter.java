package com.stylo.api_agendamento.adapters.outbound.persistence.google;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.GoogleConnectionStatus;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.IGoogleTokenRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.usecases.dto.ExternalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements ICalendarProvider {

    private final IGoogleTokenRepository tokenRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceProviderRepository serviceProviderRepository;

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${stylo.webhook-secret}")
    private String webhookSecret;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Stylo SaaS";
    private static final String DEFAULT_TIMEZONE = "America/Sao_Paulo";

    @Override
    public String createEvent(Appointment appointment) {
        try {
            Calendar service = getGoogleServiceForProfessional(appointment.getProfessionalId());

            ZoneId zoneId = appointment.getZoneId() != null ? appointment.getZoneId() : ZoneId.of(DEFAULT_TIMEZONE);
            ZonedDateTime startZoned = appointment.getStartTime().atZone(zoneId);
            ZonedDateTime endZoned = appointment.getEndTime().atZone(zoneId);

            Event event = new Event()
                    .setSummary(
                            "✂️ " + appointment.getClientName() + " - " + appointment.getServices().get(0).getName())
                    .setDescription(buildDescription(appointment))
                    .setLocation("Stylo - " + appointment.getBusinessName());

            event.setStart(new EventDateTime()
                    .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                    .setTimeZone(zoneId.getId()));

            event.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                    .setTimeZone(zoneId.getId()));

            Event executedEvent = service.events().insert("primary", event).execute();
            log.info("Evento Google criado: ID {} para profissional {}", executedEvent.getId(),
                    appointment.getProfessionalId());

            return executedEvent.getId();

        } catch (BusinessException be) {
            log.warn("Integração Google interrompida para {}: {}", appointment.getProfessionalId(), be.getMessage());
            throw be;
        } catch (Exception e) {
            log.error("Erro técnico ao sincronizar com Google: {}", e.getMessage());
            throw new RuntimeException("Falha na API Google Calendar", e);
        }
    }

    @Override
    public void deleteEvent(UUID professionalId, String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank())
            return;

        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);
            service.events().delete("primary", externalEventId).execute();
            log.info("Evento removido do Google: {}", externalEventId);

        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 410) {
                log.info("Evento Google já inexistente: {}", externalEventId);
            } else {
                throw new RuntimeException("Erro ao deletar evento Google", e);
            }
        } catch (Exception e) {
            log.error("Erro ao remover evento Google: {}", e.getMessage());
        }
    }

    @Override
    public List<ExternalEvent> fetchRecentEvents(UUID professionalId) {
        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);
            ZoneId professionalZone = getProfessionalTimeZone(professionalId);
            DateTime now = new DateTime(System.currentTimeMillis());

            Events events = service.events().list("primary")
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            if (events.getItems() == null)
                return Collections.emptyList();

            return events.getItems().stream()
                    .map(evt -> toExternalEvent(evt, professionalZone))
                    .toList();

        } catch (Exception e) {
            log.error("Falha ao buscar agenda externa do profissional {}", professionalId);
            return Collections.emptyList();
        }
    }

    @Override
    public void watchCalendar(UUID professionalId, String webhookUrl) {
        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);
            Channel channel = new Channel()
                    .setId(UUID.randomUUID().toString())
                    .setType("web_hook")
                    .setAddress(webhookUrl)
                    .setToken(webhookSecret);

            service.events().watch("primary", channel).execute();
        } catch (Exception e) {
            log.error("Falha ao configurar Webhook Google para {}", professionalId);
        }
    }

    // --- Métodos Privados de Apoio ---

    private String buildDescription(Appointment appt) {
        String services = appt.getServices().stream()
                .map(com.stylo.api_agendamento.core.domain.Service::getName)
                .reduce((a, b) -> a + ", " + b).orElse("");

        return "📱 Cliente: " + appt.getClientName() + "\n" +
                "💇 Serviços: " + services + "\n" +
                "📞 Telefone: " + (appt.getClientPhone() != null ? appt.getClientPhone().value() : "Não informado");
    }

    private ExternalEvent toExternalEvent(Event googleEvent, ZoneId zoneId) {
        var gStart = googleEvent.getStart().getDateTime();
        var gEnd = googleEvent.getEnd().getDateTime();
        LocalDateTime start;
        LocalDateTime end;

        if (gStart == null) { // Eventos de dia inteiro
            long startMillis = googleEvent.getStart().getDate().getValue();
            start = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDateTime();
            end = start.plusDays(1).minusMinutes(1);
        } else {
            start = Instant.ofEpochMilli(gStart.getValue()).atZone(zoneId).toLocalDateTime();
            end = Instant.ofEpochMilli(gEnd.getValue()).atZone(zoneId).toLocalDateTime();
        }
        return new ExternalEvent(googleEvent.getId(), googleEvent.getSummary(), start, end);
    }

    private ZoneId getProfessionalTimeZone(UUID professionalId) {
        return professionalRepository.findById(professionalId)
                .map(Professional::getServiceProviderId)
                .flatMap(serviceProviderRepository::findById)
                .map(sp -> {
                    try {
                        return ZoneId.of(sp.getTimeZone());
                    } catch (Exception e) {
                        return ZoneId.of(DEFAULT_TIMEZONE);
                    }
                })
                .orElse(ZoneId.of(DEFAULT_TIMEZONE));
    }

    private Calendar getGoogleServiceForProfessional(UUID professionalId) {
        var tokenData = tokenRepository.findByProfessionalId(professionalId)
                .orElseThrow(() -> new BusinessException("Google Calendar não conectado."));

        if (tokenData.status() == GoogleConnectionStatus.DISCONNECTED) {
            throw new BusinessException("Integração Google pausada para este profissional.");
        }

        if (tokenData.expiresAt().isBefore(LocalDateTime.now().plusMinutes(1))) {
            tokenData = refreshGoogleToken(professionalId, tokenData.refreshToken());
        }

        return createGoogleServiceClient(tokenData.accessToken());
    }

    private Calendar createGoogleServiceClient(String accessToken) {
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod())
                    .setAccessToken(accessToken);
            return new Calendar.Builder(transport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new BusinessException("Erro interno ao inicializar cliente Google.");
        }
    }

    private IGoogleTokenRepository.TokenData refreshGoogleToken(UUID professionalId, String refreshToken) {
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            TokenResponse response = new GoogleRefreshTokenRequest(transport, JSON_FACTORY, refreshToken, clientId,
                    clientSecret).execute();

            LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(response.getExpiresInSeconds() - 60);

            tokenRepository.saveTokens(professionalId, response.getAccessToken(), refreshToken, newExpiresAt);

            return new IGoogleTokenRepository.TokenData(response.getAccessToken(), refreshToken, newExpiresAt,
                    GoogleConnectionStatus.CONNECTED);

        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 400 || e.getStatusCode() == 401) {
                log.warn("Acesso Google revogado para {}. Desconectando integração.", professionalId);
                tokenRepository.markAsDisconnected(professionalId);
                throw new BusinessException("Conexão com Google Calendar perdida. É necessário reconectar.");
            }
            throw new RuntimeException("Erro ao renovar acesso Google", e);
        } catch (Exception e) {
            throw new RuntimeException("Falha na renovação do token Google", e);
        }
    }

    @Override
    public void updateEvent(Appointment appointment, String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) {
            // Se não tinha ID antes, tentamos criar um novo para manter a sincronia
            createEvent(appointment);
            return;
        }

        try {
            Calendar service = getGoogleServiceForProfessional(appointment.getProfessionalId());

            ZoneId zoneId = appointment.getZoneId() != null ? appointment.getZoneId() : ZoneId.of(DEFAULT_TIMEZONE);
            ZonedDateTime startZoned = appointment.getStartTime().atZone(zoneId);
            ZonedDateTime endZoned = appointment.getEndTime().atZone(zoneId);

            // Buscamos o evento atual para preservar campos que não controlamos
            Event event = service.events().get("primary", externalEventId).execute();

            // Atualizamos apenas o que mudou
            event.setSummary("✂️ " + appointment.getClientName() + " - " + appointment.getServices().get(0).getName())
                    .setDescription(buildDescription(appointment))
                    .setStart(new EventDateTime()
                            .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                            .setTimeZone(zoneId.getId()))
                    .setEnd(new EventDateTime()
                            .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                            .setTimeZone(zoneId.getId()));

            service.events().update("primary", externalEventId, event).execute();
            log.info("Evento Google atualizado: ID {} para profissional {}", externalEventId,
                    appointment.getProfessionalId());

        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Evento {} não encontrado no Google para atualização. Criando novo.", externalEventId);
                createEvent(appointment);
            } else {
                throw new RuntimeException("Erro ao atualizar evento no Google Calendar", e);
            }
        } catch (Exception e) {
            log.error("Falha técnica ao atualizar evento Google: {}", e.getMessage());
            throw new RuntimeException("Erro na integração Google Calendar", e);
        }
    }
}