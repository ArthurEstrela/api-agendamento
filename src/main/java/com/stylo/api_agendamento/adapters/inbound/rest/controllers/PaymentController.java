package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import com.stylo.api_agendamento.core.usecases.HandlePaymentWebhookUseCase;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Pagamentos e Assinaturas (SaaS)", description = "Webhooks e processamento de eventos do gateway de pagamento (ex: Stripe)")
public class PaymentController {

    private final HandlePaymentWebhookUseCase handlePaymentWebhookUseCase;
    private final IPaymentProvider paymentProvider;

    @Operation(summary = "Webhook de Pagamentos", description = "Recebe eventos do gateway de pagamento (pagamento de agendamento, falha de assinatura SaaS, etc). A segurança é feita rigorosamente via verificação de assinatura criptográfica HMAC.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento processado (ou ignorado por idempotência) com sucesso"),
            @ApiResponse(responseCode = "400", description = "Payload vazio, mal formatado ou assinatura ausente"),
            @ApiResponse(responseCode = "403", description = "Assinatura digital inválida (Tentativa de fraude bloqueada)"),
            @ApiResponse(responseCode = "500", description = "Erro interno. O Gateway agendará uma retentativa (Exponential Backoff).")
    })
    @PostMapping("/webhook")
    // NOTA: Este endpoint nunca deve ter @PreAuthorize, pois é o Stripe que o chama de forma anónima, 
    // validando-se exclusivamente pela Assinatura Criptográfica.
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestBody String rawPayload // Recebe o JSON bruto (String) para garantir a precisão do cálculo de Hash
    ) {
        
        // 1. Validação de Entrada Rápida (Protege o Provider de NullPointerExceptions)
        if (signature == null || signature.isBlank()) {
            log.warn("Tentativa de chamada ao webhook sem cabeçalho de assinatura digital.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if (rawPayload == null || rawPayload.isBlank()) {
            log.warn("Payload vazio recebido no webhook.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            // 2. O Provider valida a assinatura (HMAC SHA-256) e converte para o DTO interno.
            // Isso isola perfeitamente o nosso Controller da biblioteca específica do Stripe (Clean Architecture).
            PaymentWebhookInput input = paymentProvider.validateAndParseWebhook(rawPayload, signature);

            // 3. Chama a orquestração de negócios (Renovação de SaaS, Confirmação de Agendamentos, Idempotência)
            handlePaymentWebhookUseCase.execute(input);

            // O retorno 200 OK informa ao Stripe que recebemos e processamos a mensagem.
            return ResponseEntity.ok().build();

        } catch (SecurityException e) {
            // Assinatura não confere com o nosso webhookSecret
            log.error("🚨 ALERTA DE SEGURANÇA: Assinatura de webhook inválida. Possível tentativa de fraude: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        } catch (IllegalArgumentException e) {
            // O Payload não era um JSON válido ou não possuía os campos estruturais mínimos
            log.error("Erro ao fazer parse do payload do webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (Exception e) {
            // Retornar 500 faz o gateway (Stripe) agendar retentativas automáticas nos próximos dias.
            // Isto é vital para não perdermos pagamentos de assinaturas (SaaS) se o nosso banco de dados estiver em baixo.
            log.error("❌ Erro interno ao processar webhook (O Stripe fará retentativa): ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}