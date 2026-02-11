package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.SyncExternalCalendarUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/webhooks/google-calendar")
@RequiredArgsConstructor
public class GoogleCalendarWebhookController {

    private final SyncExternalCalendarUseCase syncUseCase;

    @PostMapping("/notifications")
    public ResponseEntity<Void> handleNotification(
            @RequestHeader("X-Goog-Resource-ID") String resourceId,
            @RequestHeader("X-Goog-Resource-State") String state) {

        if ("sync".equalsIgnoreCase(state)) return ResponseEntity.ok().build();

        // O Google avisa que algo mudou, o Use Case trata de buscar e bloquear
        syncUseCase.execute(resourceId); 

        return ResponseEntity.accepted().build();
    }
}