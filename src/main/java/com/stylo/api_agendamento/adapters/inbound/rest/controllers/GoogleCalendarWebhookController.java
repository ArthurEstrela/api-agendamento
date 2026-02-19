package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.SyncExternalCalendarUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1/webhooks/google-calendar")
@RequiredArgsConstructor
@Tag(name = "Webhooks do Google", description = "Endpoints de callback ocultos/públicos para notificações Push do Google Calendar")
public class GoogleCalendarWebhookController {

    private final SyncExternalCalendarUseCase syncUseCase;

    @Value("${stylo.webhook-secret}")
    private String webhookSecret; // O segredo que o nosso sistema enviou na criação do Watch

    @Operation(summary = "Receber Notificações Push", description = "Endpoint chamado exclusivamente pelos servidores do Google sempre que a agenda de um profissional é alterada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificação processada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Acesso não autorizado (Token inválido)")
    })
    @PostMapping("/notifications")
    public ResponseEntity<Void> handleNotification(
            @RequestHeader("X-Goog-Channel-ID") String channelId,     // ✨ NOVO: É aqui que o Google nos devolve o professionalId
            @RequestHeader("X-Goog-Resource-ID") String resourceId,   // ID da subscrição gerado pelo Google
            @RequestHeader("X-Goog-Resource-State") String state,     // "sync", "exists", etc.
            @RequestHeader(value = "X-Goog-Channel-Token", required = false) String token // Token de segurança
    ) {
        // 1. Validação de Segurança (O Porteiro)
        if (token == null || !token.equals(webhookSecret)) {
            log.warn("Tentativa de acesso não autorizado ao Webhook! ChannelID: {}, ResourceID: {}", channelId, resourceId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Handshake do Google (Confirmação inicial de criação do Watch)
        if ("sync".equalsIgnoreCase(state)) {
            log.info("Handshake de sincronização recebido do Google para o ChannelID (Profissional): {}", channelId);
            return ResponseEntity.ok().build();
        }

        // 3. Processamento Seguro e Tipado
        log.info("Notificação de atualização recebida. Estado: {}. Sincronizando agenda do ChannelID: {}", state, channelId);
        
        try {
            // Transformamos o ChannelID que o Google nos enviou de volta num UUID válido
            UUID professionalId = UUID.fromString(channelId);
            
            // Agora sim o Caso de Uso recebe o tipo correto!
            syncUseCase.execute(professionalId); 
            
        } catch (IllegalArgumentException e) {
            log.error("ChannelID recebido não é um UUID válido: {}. Ignorando notificação.", channelId);
            // Retornamos OK na mesma para o Google parar de fazer retentativas de erro
        } catch (Exception e) {
            log.error("Erro ao processar webhook para o profissional {}: {}", channelId, e.getMessage(), e);
            // Retornamos OK. Em webhooks, erros de negócio internos não devem fazer o emissor tentar novamente infinitamente
        }

        return ResponseEntity.ok().build();
    }
}