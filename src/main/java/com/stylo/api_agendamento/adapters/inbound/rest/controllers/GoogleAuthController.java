package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.stylo.api_agendamento.core.ports.IGoogleTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequestMapping("/v1/auth/google")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final IGoogleTokenRepository tokenRepository;

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
                Collections.singleton("https://www.googleapis.com/auth/calendar")
        ).setAccessType("offline").setApprovalPrompt("force").build();

        // Passamos o professionalId no "state" para saber quem está logando no callback
        String url = flow.newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .setState(professionalId)
                .build();

        return new RedirectView(url);
    }

    @GetMapping("/callback")
    public String callback(@RequestParam String code, @RequestParam String state) throws Exception {
        var flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientId, clientSecret,
                Collections.singleton("https://www.googleapis.com/auth/calendar")
        ).build();

        GoogleTokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri(redirectUri)
                .execute();

        // Salva os tokens vinculados ao professionalId (que veio no state)
        tokenRepository.saveTokens(
                state,
                response.getAccessToken(),
                response.getRefreshToken(),
                LocalDateTime.now().plusSeconds(response.getExpiresInSeconds())
        );

        return "Agenda conectada com sucesso! Você já pode fechar esta aba.";
    }
}