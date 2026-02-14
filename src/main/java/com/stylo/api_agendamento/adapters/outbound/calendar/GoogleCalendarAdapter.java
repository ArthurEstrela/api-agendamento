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
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.IGoogleTokenRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements ICalendarProvider {

    private final IGoogleTokenRepository tokenRepository;
    // ✨ Injetado para buscar o TimeZone correto ao ler eventos (fetchRecentEvents)
    private final IProfessionalRepository professionalRepository;

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

            // 1. Definição do Fuso Horário
            // Usa o ZoneId armazenado no agendamento. Se não houver, usa fallback.
            ZoneId zoneId = appointment.getZoneId();

            // 2. Conversão de LocalDateTime (sem fuso) para ZonedDateTime (com fuso)
            // Isso diz: "Essa data/hora (10:00) é no fuso de São Paulo"
            ZonedDateTime startZoned = appointment.getStartTime().atZone(zoneId);
            ZonedDateTime endZoned = appointment.getEndTime().atZone(zoneId);

            Event event = new Event()
                    .setSummary("✂️ " + appointment.getClientName() + " - " + appointment.getServices().get(0).getName())
                    .setDescription(buildDescription(appointment))
                    .setLocation("Stylo - " + appointment.getBusinessName());

            // 3. Criação do EventDateTime do Google
            // Convertemos para Epoch Millis (UTC absoluto) mas enviamos o TimeZone junto.
            // Isso garante que o Google mostre a hora certa, independente de onde esteja o servidor.
            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(startZoned.toInstant().toEpochMilli()))
                    .setTimeZone(zoneId.getId());

            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(endZoned.toInstant().toEpochMilli()))
                    .setTimeZone(zoneId.getId());

            event.setStart(start);
            event.setEnd(end);

            Event executedEvent = service.events().insert("primary", event).execute();
            log.info("Evento Google criado com sucesso. ID: {} | Fuso: {}", executedEvent.getId(), zoneId);

            return executedEvent.getId();

        } catch (Exception e) {
            // Loga o erro mas não quebra o fluxo (O agendamento no Stylo já existe)
            log.error("Erro ao criar evento no Google Calendar: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void deleteEvent(String professionalId, String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) return;

        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);
            service.events().delete("primary", externalEventId).execute();
            log.info("Evento removido do Google Calendar. ID: {}", externalEventId);

        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("410"))) {
                log.warn("Evento Google já não existe (Idempotência): {}", externalEventId);
            } else {
                log.error("Falha ao deletar evento Google: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<ExternalEvent> fetchRecentEvents(String professionalId) {
        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);

            // ✨ Busca o fuso do profissional para interpretar corretamente os eventos lidos
            ZoneId professionalZone = getProfessionalTimeZone(professionalId);

            DateTime now = new DateTime(System.currentTimeMillis());
            
            // Busca eventos a partir de agora
            Events events = service.events().list("primary")
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            if (events.getItems() == null) return Collections.emptyList();

            return events.getItems().stream()
                    .map(evt -> toExternalEvent(evt, professionalZone))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Erro ao buscar eventos Google para {}: {}", professionalId, e.getMessage());
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
                    .setToken(webhookSecret); 

            service.events().watch("primary", channel).execute();
            log.info("Webhook Google ativado para: {}", professionalId);
        } catch (Exception e) {
            log.error("Erro ao configurar watch no Google: {}", e.getMessage());
        }
    }

    // --- Métodos Privados ---

    private String buildDescription(Appointment appt) {
        String services = appt.getServices().stream()
                .map(com.stylo.api_agendamento.core.domain.Service::getName)
                .reduce((a, b) -> a + ", " + b).orElse("");
        return "Cliente: " + appt.getClientName() + "\n" +
               "Serviços: " + services + "\n" +
               "Telefone: " + appt.getClientPhone().getNumber();
    }

    /**
     * Converte o evento do Google (que pode estar em qualquer fuso ou UTC)
     * para o LocalDateTime do sistema, respeitando o fuso do Profissional.
     */
    private ExternalEvent toExternalEvent(Event googleEvent, ZoneId zoneId) {
        var googleStart = googleEvent.getStart().getDateTime();
        var googleEnd = googleEvent.getEnd().getDateTime();

        LocalDateTime start;
        LocalDateTime end;

        // Tratamento para eventos de dia inteiro (All-day events)
        if (googleStart == null) {
            // Se for dia inteiro, o Google manda 'date' (yyyy-MM-dd)
            // Assumimos início do dia no fuso do profissional
            if (googleEvent.getStart().getDate() != null) {
                 // Lógica simplificada para dia inteiro
                 long startMillis = googleEvent.getStart().getDate().getValue();
                 start = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDateTime();
                 end = start.plusDays(1); // Dia inteiro dura 1 dia
            } else {
                 // Fallback extremo
                 start = LocalDateTime.now();
                 end = LocalDateTime.now().plusHours(1);
            }
        } else {
            // Evento normal com hora marcada
            // Convertemos o Instant (UTC Millis) para o LocalDateTime no fuso do profissional
            start = Instant.ofEpochMilli(googleStart.getValue()).atZone(zoneId).toLocalDateTime();
            end = Instant.ofEpochMilli(googleEnd.getValue()).atZone(zoneId).toLocalDateTime();
        }

        return new ExternalEvent(googleEvent.getId(), googleEvent.getSummary(), start, end);
    }

    private ZoneId getProfessionalTimeZone(String professionalId) {
        return professionalRepository.findById(professionalId)
                .map(Professional::getServiceProvider) // Assume que Profissional tem ServiceProvider carregado
                .map(sp -> {
                    try {
                        return ZoneId.of(sp.getTimeZone());
                    } catch (Exception e) {
                        return ZoneId.of(DEFAULT_TIMEZONE);
                    }
                })
                .orElse(ZoneId.of(DEFAULT_TIMEZONE));
    }

    private Calendar getGoogleServiceForProfessional(String professionalId) {
        var tokenData = tokenRepository.findByProfessionalId(professionalId)
                .orElseThrow(() -> new BusinessException("Google Calendar não conectado."));

        if (tokenData.expiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
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
            throw new BusinessException("Falha interna ao criar cliente Google.");
        }
    }

    private IGoogleTokenRepository.TokenData refreshGoogleToken(String professionalId, String refreshToken) {
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            TokenResponse response = new GoogleRefreshTokenRequest(
                    transport, JSON_FACTORY, refreshToken, clientId, clientSecret)
                    .execute();

            // Adiciona margem de segurança
            LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(response.getExpiresInSeconds() - 60);

            tokenRepository.saveTokens(
                    professionalId,
                    response.getAccessToken(),
                    refreshToken,
                    newExpiresAt);

            return new IGoogleTokenRepository.TokenData(response.getAccessToken(), refreshToken, newExpiresAt);

        } catch (com.google.api.client.http.HttpResponseException e) {
            if (e.getStatusCode() == 400 || e.getStatusCode() == 401) {
                log.error("Token Google revogado para: {}", professionalId);
                // Aqui seria ideal disparar um evento de "Desconexão" para atualizar o status no banco
            }
            throw new BusinessException("Conexão com Google expirada. Reconecte sua conta.");
        } catch (Exception e) {
            throw new BusinessException("Erro ao renovar token Google.");
        }
    }
}