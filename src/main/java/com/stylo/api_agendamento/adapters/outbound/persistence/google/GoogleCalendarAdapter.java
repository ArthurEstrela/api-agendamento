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
import java.util.stream.Collectors;

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

            ZoneId zoneId = appointment.getZoneId();
            ZonedDateTime startZoned = appointment.getStartTime().atZone(zoneId);
            ZonedDateTime endZoned = appointment.getEndTime().atZone(zoneId);

            Event event = new Event()
                    .setSummary("✂️ " + appointment.getClientName() + " - " + appointment.getServices().get(0).getName())
                    .setDescription(buildDescription(appointment))
                    .setLocation("Stylo - " + appointment.getBusinessName());

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

        } catch (BusinessException be) {
            // ✨ MELHORIA: Erros de regra de negócio (Token revogado/desconectado) não devem gerar retry
            log.warn("Integração Google pausada para o profissional {}: {}", appointment.getProfessionalId(), be.getMessage());
            throw be; 
        } catch (Exception e) {
            // ✨ MELHORIA: Erros técnicos (Timeout, 500, Rede) são relançados 
            // para que o AppointmentEventListener capture e agende o Retry.
            log.error("Erro técnico ao criar evento Google: {}", e.getMessage());
            throw new RuntimeException("Falha na integração Google: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteEvent(String professionalId, String externalEventId) {
        if (externalEventId == null || externalEventId.isBlank()) return;

        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);
            service.events().delete("primary", externalEventId).execute();
            log.info("Evento removido do Google Calendar. ID: {}", externalEventId);

        } catch (BusinessException be) {
            log.warn("Não foi possível deletar evento Google (status desconectado): {}", be.getMessage());
            // Não relançamos aqui pois deletar é "best effort" neste contexto
        } catch (HttpResponseException e) {
            // ✨ MELHORIA: Idempotência
            if (e.getStatusCode() == 404 || e.getStatusCode() == 410) {
                log.info("Evento Google já não existe (404/410), considerado deletado: {}", externalEventId);
            } else {
                log.error("Erro HTTP Google ao deletar: {}", e.getStatusCode());
                throw new RuntimeException("Erro ao deletar evento Google", e);
            }
        } catch (Exception e) {
            log.error("Falha genérica ao deletar evento Google: {}", e.getMessage());
            throw new RuntimeException("Erro ao deletar evento Google", e);
        }
    }

    @Override
    public List<ExternalEvent> fetchRecentEvents(String professionalId) {
        try {
            Calendar service = getGoogleServiceForProfessional(professionalId);
            ZoneId professionalZone = getProfessionalTimeZone(professionalId);
            DateTime now = new DateTime(System.currentTimeMillis());
            
            Events events = service.events().list("primary")
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            if (events.getItems() == null) return Collections.emptyList();

            return events.getItems().stream()
                    .map(evt -> toExternalEvent(evt, professionalZone))
                    .collect(Collectors.toList());

        } catch (BusinessException be) {
             // Se não estiver conectado, retorna lista vazia sem erro
             return Collections.emptyList();
        } catch (Exception e) {
            log.error("Erro ao buscar eventos Google para {}: {}", professionalId, e.getMessage());
            // Para leitura, preferimos retornar vazio a quebrar a tela do usuário
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
            // Webhook falhando não deve travar o sistema, apenas logamos
        }
    }

    // --- Métodos Privados ---

    private String buildDescription(Appointment appt) {
        String services = appt.getServices().stream()
                .map(com.stylo.api_agendamento.core.domain.Service::getName)
                .reduce((a, b) -> a + ", " + b).orElse("");
        
        return "Cliente: " + appt.getClientName() + "\n" +
               "Serviços: " + services + "\n" +
               "Telefone: " + appt.getClientPhone().getValue();
    }

    private ExternalEvent toExternalEvent(Event googleEvent, ZoneId zoneId) {
        var googleStart = googleEvent.getStart().getDateTime();
        var googleEnd = googleEvent.getEnd().getDateTime();
        LocalDateTime start;
        LocalDateTime end;

        // Lógica para lidar com eventos de "Dia Inteiro" (All Day Events)
        if (googleStart == null) {
            if (googleEvent.getStart().getDate() != null) {
                 long startMillis = googleEvent.getStart().getDate().getValue();
                 start = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDateTime();
                 // Eventos de dia inteiro no Google terminam no início do dia seguinte, mas para display ajustamos
                 end = start.plusDays(1).minusMinutes(1); 
            } else {
                 start = LocalDateTime.now();
                 end = LocalDateTime.now().plusHours(1);
            }
        } else {
            start = Instant.ofEpochMilli(googleStart.getValue()).atZone(zoneId).toLocalDateTime();
            end = Instant.ofEpochMilli(googleEnd.getValue()).atZone(zoneId).toLocalDateTime();
        }
        return new ExternalEvent(googleEvent.getId(), googleEvent.getSummary(), start, end);
    }

    private ZoneId getProfessionalTimeZone(String professionalId) {
        return professionalRepository.findById(professionalId)
                .map(Professional::getServiceProviderId) 
                .flatMap(serviceProviderRepository::findById) 
                .map(sp -> {
                    try { return ZoneId.of(sp.getTimeZone()); } 
                    catch (Exception e) { return ZoneId.of(DEFAULT_TIMEZONE); }
                })
                .orElse(ZoneId.of(DEFAULT_TIMEZONE));
    }

    private Calendar getGoogleServiceForProfessional(String professionalId) {
        var tokenData = tokenRepository.findByProfessionalId(professionalId)
                .orElseThrow(() -> new BusinessException("Google Calendar não conectado."));

        if (tokenData.status() == GoogleConnectionStatus.DISCONNECTED) {
            throw new BusinessException("Integração Google pausada. Token inválido.");
        }

        // Renova token se expirar em menos de 1 minuto
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
            throw new BusinessException("Falha interna ao criar cliente Google.");
        }
    }

    private IGoogleTokenRepository.TokenData refreshGoogleToken(String professionalId, String refreshToken) {
        try {
            var transport = GoogleNetHttpTransport.newTrustedTransport();
            TokenResponse response = new GoogleRefreshTokenRequest(
                    transport, JSON_FACTORY, refreshToken, clientId, clientSecret)
                    .execute();

            LocalDateTime newExpiresAt = LocalDateTime.now().plusSeconds(response.getExpiresInSeconds() - 60);

            tokenRepository.saveTokens(
                    professionalId,
                    response.getAccessToken(),
                    refreshToken,
                    newExpiresAt);

            return new IGoogleTokenRepository.TokenData(
                    response.getAccessToken(), 
                    refreshToken, 
                    newExpiresAt, 
                    GoogleConnectionStatus.CONNECTED);

        } catch (HttpResponseException e) {
            // ✨ MELHORIA: Tratamento específico para revogação de acesso
            if (e.getStatusCode() == 400 || e.getStatusCode() == 401) {
                log.warn("Token Google revogado ou inválido para o profissional {}. Marcando como DESCONECTADO.", professionalId);
                
                tokenRepository.markAsDisconnected(professionalId);
                
                throw new BusinessException("A conexão com o Google expirou. Por favor, reconecte sua conta.");
            }
            // Outros erros HTTP (500, etc) são relançados para tentativa posterior
            throw new RuntimeException("Erro de comunicação com o Google ao renovar token.", e);

        } catch (Exception e) {
            log.error("Erro desconhecido ao renovar token Google: {}", e.getMessage());
            throw new RuntimeException("Falha ao renovar token Google.", e);
        }
    }
}