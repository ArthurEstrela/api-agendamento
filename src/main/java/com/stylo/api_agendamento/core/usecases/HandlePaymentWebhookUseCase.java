// src/main/java/com/stylo/api_agendamento/core/usecases/HandlePaymentWebhookUseCase.java
package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.ICacheService;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class HandlePaymentWebhookUseCase {

    private final IServiceProviderRepository serviceProviderRepository;
    private final IAppointmentRepository appointmentRepository;
    private final ICacheService cacheService; // ✨ Adicionado para Idempotência

    @Transactional
    public void execute(PaymentWebhookInput input) {
        String idempotencyKey = "webhook_event:" + input.eventId();

        Object cachedEvent = cacheService.get(idempotencyKey);
        if (cachedEvent != null) {
            log.info("Evento de webhook {} já processado anteriormente.", input.eventId());
            return;
        }

        log.info("Processando novo evento webhook: {} | Status: {}", input.eventType(), input.status());

        String paymentContext = input.metadata().getOrDefault("context", "UNKNOWN");

        switch (paymentContext) {
            case "APPOINTMENT" -> handleAppointmentPayment(input);
            case "SUBSCRIPTION" -> handleSubscriptionPayment(input);
            default -> log.warn("Contexto de pagamento desconhecido: {}", paymentContext);
        }

        // ✨ Chamada corrigida conforme a interface ICacheService
        cacheService.set(idempotencyKey, "PROCESSED", 1440);
    }

    private void handleAppointmentPayment(PaymentWebhookInput input) {
        if (!isPaymentSuccessful(input.status())) {
            log.info("Pagamento do agendamento {} não aprovado. Status: {}", input.gatewayPaymentId(), input.status());
            return;
        }

        String appointmentId = input.metadata().get("appointmentId");
        if (appointmentId == null) {
            log.error("Erro crítico: Webhook de agendamento sem 'appointmentId' no metadata.");
            return;
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + appointmentId));

        // Registra o ID externo e muda status para SCHEDULED/PAID internamente
        appointment.confirmPayment(input.gatewayPaymentId());

        appointmentRepository.save(appointment);
        log.info("Pagamento confirmado e agendamento atualizado: {}", appointment.getId());
    }

    private void handleSubscriptionPayment(PaymentWebhookInput input) {
        String providerId = input.metadata().get("providerId");
        if (providerId == null) {
            log.error("Erro crítico: Webhook de assinatura sem 'providerId' no metadata.");
            return;
        }

        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Provedor não encontrado: " + providerId));

        if (isPaymentSuccessful(input.status())) {
            provider.updateSubscription("ACTIVE");
            log.info("Assinatura do provedor {} reativada com sucesso.", provider.getBusinessName());
        } else if ("invoice.payment_failed".equals(input.eventType())) {
            handleSubscriptionFailure(provider);
        }

        serviceProviderRepository.save(provider);
    }

    private void handleSubscriptionFailure(ServiceProvider provider) {
        // Lógica de carência (Grace Period) antes do bloqueio total
        if (provider.isSubscriptionActive() && !"GRACE_PERIOD".equals(provider.getSubscriptionStatus())) {
            provider.startGracePeriod(3);
            log.warn("Falha no pagamento. Provedor {} entrou em Grace Period (3 dias).", provider.getBusinessName());
        } else {
            provider.updateSubscription("EXPIRED");
            log.error("Assinatura do provedor {} expirada por falta de pagamento.", provider.getId());
        }
    }

    private boolean isPaymentSuccessful(String status) {
        return "succeeded".equalsIgnoreCase(status)
                || "paid".equalsIgnoreCase(status)
                || "approved".equalsIgnoreCase(status);
    }
}