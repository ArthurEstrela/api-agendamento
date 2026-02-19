package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.ServiceProvider.SubscriptionStatus;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.ICacheService;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.usecases.dto.PaymentWebhookInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class HandlePaymentWebhookUseCase {

    private final IServiceProviderRepository serviceProviderRepository;
    private final IAppointmentRepository appointmentRepository;
    private final ICacheService cacheService;

    @Transactional
    public void execute(PaymentWebhookInput input) {
        // 1. Chave de Idempotência (Evita processar o mesmo evento duas vezes)
        String idempotencyKey = "webhook_event:" + input.eventId();
        Optional<String> cached = cacheService.get(idempotencyKey, String.class);
        
        if (cached.isPresent()) {
            log.info("Evento de webhook {} já processado. Ignorando.", input.eventId());
            return;
        }

        log.info("Webhook Recebido: {} | Status: {}", input.eventType(), input.status());

        // 2. Orquestração baseada no contexto (Metadata do Stripe)
        String context = input.metadata().getOrDefault("context", "UNKNOWN");

        switch (context) {
            case "APPOINTMENT" -> handleAppointmentPayment(input);
            case "SUBSCRIPTION" -> handleSubscriptionPayment(input);
            default -> log.warn("Contexto de pagamento '{}' não reconhecido pela plataforma Stylo.", context);
        }

        // 3. Marca como processado por 24 horas (1440 minutos)
        cacheService.set(idempotencyKey, "PROCESSED", 1440);
    }

    private void handleAppointmentPayment(PaymentWebhookInput input) {
        if (!isPaymentSuccessful(input.status())) return;

        String appointmentIdStr = input.metadata().get("appointmentId");
        if (appointmentIdStr == null) {
            log.error("Erro: Webhook 'APPOINTMENT' sem appointmentId no metadata.");
            return;
        }

        UUID appointmentId = UUID.fromString(appointmentIdStr);
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado: " + appointmentId));

        // Confirma o pagamento no domínio (Muda status para SCHEDULED/PAID)
        appointment.confirmPayment(input.gatewayPaymentId());
        appointmentRepository.save(appointment);
        
        log.info("Agendamento {} pago e confirmado via Webhook.", appointment.getId());
    }

    private void handleSubscriptionPayment(PaymentWebhookInput input) {
        String providerIdStr = input.metadata().get("providerId");
        if (providerIdStr == null) return;

        UUID providerId = UUID.fromString(providerIdStr);
        ServiceProvider provider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Provedor não encontrado: " + providerId));

        if (isPaymentSuccessful(input.status())) {
            // Reativa a assinatura
            provider.updateSubscriptionStatus(SubscriptionStatus.ACTIVE);
            log.info("Assinatura do provedor {} reativada com sucesso.", provider.getBusinessName());
        } 
        else if ("invoice.payment_failed".equals(input.eventType())) {
            handleSubscriptionFailure(provider);
        }

        serviceProviderRepository.save(provider);
    }

    private void handleSubscriptionFailure(ServiceProvider provider) {
        // Se já estava ativo, entra em Grace Period (Carência de 3 dias antes de bloquear)
        if (provider.isSubscriptionActive() && provider.getSubscriptionStatus() != SubscriptionStatus.GRACE_PERIOD) {
            provider.startGracePeriod(3);
            log.warn("Falha no pagamento de {}. Entrando em Grace Period.", provider.getBusinessName());
        } else {
            // Se já estava em carência ou expirado, bloqueia total
            provider.updateSubscriptionStatus(SubscriptionStatus.EXPIRED);
            log.error("Assinatura do provedor {} bloqueada por falta de pagamento recorrente.", provider.getId());
        }
    }

    private boolean isPaymentSuccessful(String status) {
        return "succeeded".equalsIgnoreCase(status) 
            || "paid".equalsIgnoreCase(status) 
            || "approved".equalsIgnoreCase(status);
    }
}