package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.SyncExternalCalendarUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/webhooks/google-calendar")
@RequiredArgsConstructor
public class GoogleCalendarWebhookController {

    private final SyncExternalCalendarUseCase syncUseCase;

    @Value("${stylo.webhook-secret}")
    private String webhookSecret; // <--- O MESMO SEGREDO PARA VALIDAR

    @PostMapping("/notifications")
    public ResponseEntity<Void> handleNotification(
            @RequestHeader("X-Goog-Resource-ID") String resourceId,
            @RequestHeader("X-Goog-Resource-State") String state,
            @RequestHeader(value = "X-Goog-Channel-Token", required = false) String token // <--- O GOOGLE MANDA DE VOLTA AQUI
    ) {
        // 1. Validação de Segurança (O Porteiro)
        if (token == null || !token.equals(webhookSecret)) {
            log.warn("Tentativa de acesso não autorizado ao Webhook! ResourceID: {}", resourceId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Handshake do Google (Confirmação inicial)
        if ("sync".equalsIgnoreCase(state)) {
            log.info("Confirmação de sincronização recebida para ResourceID: {}", resourceId);
            return ResponseEntity.ok().build();
        }

        // 3. Processamento Seguro
        log.info("Notificação válida recebida. Atualizando agenda para ResourceID: {}", resourceId);
        
        // Aqui o UseCase deve ser capaz de achar o Profissional pelo ResourceID
        // Nota: O método execute deve lidar com a busca do profissional dono desse resourceId
        try {
            syncUseCase.execute(resourceId); 
        } catch (Exception e) {
            log.error("Erro ao processar webhook: {}", e.getMessage());
            // Retornamos OK para o Google não ficar tentando reenviar infinitamente se for erro de negócio
        }

        return ResponseEntity.ok().build();
    }
}