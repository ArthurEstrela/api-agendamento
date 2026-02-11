package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.IGoogleTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/v1/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final IGoogleTokenRepository tokenRepository;
    private final ICalendarProvider calendarProvider; // <--- ADICIONADO: Necessário para ativar o Watch

    @Value("${stylo.api-url}")
    private String apiUrl; // Ex: https://api.stylo.com

    @Value("${stylo.frontend-url}")
    private String frontendUrl; // <--- ADICIONADO: Ex: http://localhost:5173

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    @GetMapping("/connect/{professionalId}")
    public RedirectView connect(@PathVariable String professionalId) throws Exception {
        var flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientId, clientSecret,
                Collections.singleton("https://www.googleapis.com/auth/calendar"))
                .setAccessType("offline")
                .setApprovalPrompt("force") // Garante o Refresh Token
                .build();

        String url = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(professionalId)
                .build();

        return new RedirectView(url);
    }

    @GetMapping("/callback")
    public RedirectView callback(@RequestParam String code, @RequestParam String state) {
        try {
            var flow = new GoogleAuthorizationCodeFlow.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    clientId, clientSecret,
                    Collections.singleton("https://www.googleapis.com/auth/calendar")).build();

            GoogleTokenResponse response = flow.newTokenRequest(code)
                    .setRedirectUri(redirectUri)
                    .execute();

            String professionalId = state;

            // 1. Salva os tokens
            tokenRepository.saveTokens(
                    professionalId,
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    LocalDateTime.now().plusSeconds(response.getExpiresInSeconds())
            );

            // 2. Ativa o Webhook dinamicamente usando a API URL configurada
            // Isso aqui é o que estava faltando/confuso
            String webhookUrl = apiUrl + "/v1/webhooks/google-calendar/notifications";
            calendarProvider.watchCalendar(professionalId, webhookUrl);

            log.info("Google Calendar conectado com sucesso para: {}", professionalId);

            // 3. Redireciona para o Front-end (Dashboard) com sucesso
            return new RedirectView(frontendUrl + "/dashboard/settings?google=success");

        } catch (Exception e) {
            log.error("Erro ao conectar Google Calendar: {}", e.getMessage());
            // Redireciona com erro
            return new RedirectView(frontendUrl + "/dashboard/settings?google=error");
        }
    }
}