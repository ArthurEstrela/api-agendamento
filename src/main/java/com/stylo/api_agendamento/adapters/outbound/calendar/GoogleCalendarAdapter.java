package com.stylo.api_agendamento.adapters.outbound.calendar;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.IGoogleTokenRepository;
import com.stylo.api_agendamento.core.usecases.dto.ExternalEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final IGoogleTokenRepository tokenRepository;

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${stylo.webhook-secret}")
    private String webhookSecret;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Stylo SaaS";

    @Override
    public String createEvent(Appointment appointment) {
        try {
            Calendar service = getGoogleServiceForProfessional(appointment.getProfessionalId());

            Event event = new Event()
                    .setSummary(appointment.getBusinessName() + " - " + appointment.getProfessionalName())
                    .setDescription("Serviços: " + appointment.getServices().stream()
                            .map(com.stylo.api_agendamento.core.domain.Service::getName)
                            .reduce((a, b) -> a + ", " + b).orElse(""))
                    .setLocation("Agendado via Stylo App");

            // Formatação correta RFC3339
            event.setStart(
                    new EventDateTime().setDateTime(new DateTime(appointment.getStartTime().toString() + ":00Z")));
            event.setEnd(new EventDateTime().setDateTime(new DateTime(appointment.getEndTime().toString() + ":00Z")));

            Event executedEvent = service.events().insert("primary", event).execute();
            log.info("Evento criado no Google Calendar. ID: {}", executedEvent.getId());

            return executedEvent.getId(); // Retorna o ID para ser salvo na Entity
        } catch (Exception e) {
            log.error("Erro ao criar evento no Google Calendar: {}", e.getMessage());
            return null; // Retorna null para não quebrar o fluxo principal, mas avisa no log
        }
    }

    @Override
    public void deleteEvent(String professionalId, String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank())
            return;

        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);

            // "primary" refere-se à agenda principal do usuário
            service.events().delete("primary", externalEventId).execute();
            log.info("Evento removido do Google Calendar. ID: {}", externalEventId);

        } catch (Exception e) {
            // Se o erro conter 404 ou 410, significa que já foi deletado (Idempotência)
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("410"))) {
                log.warn("Tentativa de deletar evento que já não existe no Google: {}", externalEventId);
            } else {
                log.error("Falha ao deletar evento no Google Calendar: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<ExternalEvent> fetchRecentEvents(String professionalId) {
        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);

            DateTime now = new DateTime(System.currentTimeMillis());
            Events events = service.events().list("primary")
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            if (events.getItems() == null)
                return Collections.emptyList();

            return events.getItems().stream()
                    .map(this::toExternalEvent)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Erro ao buscar eventos do Google para o profissional {}: {}", professionalId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void watchCalendar(String professionalId, String webhookUrl) {
        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);

            Channel channel = new Channel()
                    .setId(UUID.randomUUID().toString())
                    .setType("web_hook")
                    .setAddress(webhookUrl)
                    .setToken(webhookSecret); // <--- AQUI ESTÁ A SEGURANÇA

            service.events().watch("primary", channel).execute();
            log.info("Webhook ativado com token de segurança para o profissional: {}", professionalId);
        } catch (Exception e) {
            log.error("Erro ao configurar watch no Google Calendar: {}", e.getMessage());
        }
    }

    private ExternalEvent toExternalEvent(Event googleEvent) {
        var googleStart = googleEvent.getStart().getDateTime();
        var googleEnd = googleEvent.getEnd().getDateTime();

        // Tratamento para eventos de dia inteiro (que não têm DateTime, apenas Date)
        if (googleStart == null) {
            // Lógica simples: se for dia inteiro, consideramos inicio do dia no fuso do
            // sistema
            return new ExternalEvent(googleEvent.getId(), googleEvent.getSummary(), LocalDateTime.now(),
                    LocalDateTime.now());
        }

        LocalDateTime start = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(googleStart.getValue()), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(googleEnd.getValue()), ZoneId.systemDefault());

        return new ExternalEvent(googleEvent.getId(), googleEvent.getSummary(), start, end);
    }

    private Calendar getGoogleServiceForProfessional(String professionalId) {
        var tokenData = tokenRepository.findByProfessionalId(professionalId)
                .orElseThrow(() -> new BusinessException("Google Calendar não conectado para este profissional."));

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

            return new Calendar.Builder(transport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            log.error("Erro ao instanciar cliente Google Calendar: {}", e.getMessage());
            throw new BusinessException("Falha na comunicação com o Google.");
        }
    }

    private IGoogleTokenRepository.TokenData refreshGoogleToken(String professionalId, String refreshToken) {
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();

            TokenResponse response = new GoogleRefreshTokenRequest(
                    transport, JSON_FACTORY, refreshToken, clientId, clientSecret)
                    .execute();

            LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(response.getExpiresInSeconds());

            tokenRepository.saveTokens(
                    professionalId,
                    response.getAccessToken(),
                    refreshToken,
                    newExpiresAt);

            return new IGoogleTokenRepository.TokenData(response.getAccessToken(), refreshToken, newExpiresAt);

        } catch (com.google.api.client.http.HttpResponseException e) {
            // Se recebermos um 400 ou 401 aqui, significa que o Refresh Token é
            // inválido/revogado.
            if (e.getStatusCode() == 400 || e.getStatusCode() == 401) {
                log.error("Refresh Token revogado ou inválido para o profissional {}. Desconectando...",
                        professionalId);
                // Opcional: tokenRepository.deleteByProfessionalId(professionalId);
                // Opcional: notificationProvider.sendAlert(professionalId, "Sua conexão com o
                // Google expirou.");
            }
            throw new BusinessException(
                    "Sua conexão com o Google expirou permanentemente. Por favor, reconecte sua agenda.");

        } catch (Exception e) {
            log.error("Erro transitório ao renovar token Google para profissional {}: {}", professionalId,
                    e.getMessage());
            throw new BusinessException("Erro de comunicação com o Google. Tente novamente.");
        }
    }

}