package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.IGoogleTokenRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/auth/google")
@RequiredArgsConstructor
@Tag(name = "Integração Google Calendar", description = "Endpoints para o fluxo de autenticação OAuth2 com o Google")
public class GoogleAuthController {

    private final IGoogleTokenRepository tokenRepository;
    private final ICalendarProvider calendarProvider; 
    
    // ✨ INJEÇÃO ADICIONADA: Necessário para Segurança e Contexto limpo
    private final IUserContext userContext; 

    @Value("${stylo.api-url}")
    private String apiUrl;

    @Value("${stylo.frontend-url}")
    private String frontendUrl;

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    @Operation(summary = "Conectar Google Calendar", description = "Inicia o fluxo OAuth2. Redireciona o profissional logado para a página de consentimento do Google.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redirecionamento para o Google efetuado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer perfil de staff/profissional)")
    })
    @GetMapping("/connect") // ✨ CORREÇÃO: ID removido da rota por segurança
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public RedirectView connect() throws Exception {
        
        // ✨ CORREÇÃO: Extraímos o ID diretamente do utilizador logado de forma segura e já tipado como UUID
        UUID professionalId = userContext.getCurrentUserId();

        var flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientId, clientSecret,
                Collections.singleton("https://www.googleapis.com/auth/calendar"))
                .setAccessType("offline")
                .setApprovalPrompt("force") // Garante o Refresh Token na primeira vez
                .build();

        String url = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(professionalId.toString()) // Passa o UUID no state (funciona como identificador na volta)
                .build();

        return new RedirectView(url);
    }

    @Operation(summary = "Callback do Google", description = "Endpoint chamado pelo Google após o consentimento. Salva os tokens e ativa o webhook (Watch).")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "Redireciona de volta para o Dashboard Front-end com status de sucesso ou erro")
    })
    @GetMapping("/callback")
    // ATENÇÃO: Este endpoint deve estar sempre como `permitAll` no SecurityConfig para o Google conseguir chamá-lo
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

            // ✨ CORREÇÃO: Reconvertemos o state de String de volta para UUID para o repositório
            UUID professionalId = UUID.fromString(state);

            // 1. Salva os tokens usando o tipo UUID corretamente
            tokenRepository.saveTokens(
                    professionalId,
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    LocalDateTime.now().plusSeconds(response.getExpiresInSeconds())
            );

            // 2. Ativa o Webhook dinamicamente usando a API URL configurada
            String webhookUrl = apiUrl + "/v1/webhooks/google-calendar/notifications";
            calendarProvider.watchCalendar(professionalId, webhookUrl);

            log.info("Google Calendar conectado com sucesso para o profissional: {}", professionalId);

            // 3. Redireciona para o Front-end (Dashboard) com a flag de sucesso
            return new RedirectView(frontendUrl + "/dashboard/settings?google=success");

        } catch (Exception e) {
            log.error("Erro grave ao conectar Google Calendar: {}", e.getMessage(), e);
            // Redireciona o Front-end com flag de erro para mostrar um Toast/Alerta
            return new RedirectView(frontendUrl + "/dashboard/settings?google=error");
        }
    }
}