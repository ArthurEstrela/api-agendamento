package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.events.AppointmentCancelledEvent;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IServiceProviderRepository providerRepository;
    private final IUserRepository userRepository;
    private final IProductRepository productRepository; // ✨ Para devolver estoque
    private final INotificationProvider notificationProvider;
    private final IPaymentProvider paymentProvider;
    private final ICalendarProvider calendarProvider;
    private final IEventPublisher eventPublisher;

    @Transactional
    public void execute(CancelAppointmentInput input) {
        // 1. Busca e validação
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Agendamento já está cancelado.");
        }

        // 2. Busca estabelecimento (para regras de cancelamento)
        ServiceProvider provider = providerRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new BusinessException("Estabelecimento não encontrado."));

        // 3. Devolução de Estoque (Se houver produtos na comanda)
        returnProductsToStock(appointment);

        // 4. Processamento Financeiro (Estorno)
        handleFinancialRefund(appointment, provider, input.isClient());

        // 5. Cancelamento no Domínio
        appointment.cancel();
        appointment.setCancellationReason(input.reason());
        appointment.setCancelledBy(input.userId());
        
        // 6. Persistência
        appointmentRepository.save(appointment);
        
        // 7. Integrações Externas
        // Remove do Google Calendar
        if (appointment.getExternalEventId() != null) {
            try {
                calendarProvider.deleteEvent(appointment.getProfessionalId(), appointment.getExternalEventId());
            } catch (Exception e) {
                log.warn("Falha não-bloqueante ao remover do Google: {}", e.getMessage());
            }
        }

        // 8. Notificações
        notifyParties(appointment, input.userId());

        // 9. ✨ Disparo para WAITLIST (Vaga liberada!)
        eventPublisher.publish(new AppointmentCancelledEvent(
                appointment.getId(),
                appointment.getProfessionalId(),
                appointment.getStartTime(),
                appointment.getEndTime()
        ));
        
        log.info("Agendamento {} cancelado com sucesso.", appointment.getId());
    }

    private void returnProductsToStock(Appointment appointment) {
        if (appointment.getProducts() == null || appointment.getProducts().isEmpty()) return;

        for (Appointment.AppointmentItem item : appointment.getProducts()) {
            try {
                productRepository.findById(item.getProductId()).ifPresent(product -> {
                    product.restoreStock(item.getQuantity());
                    productRepository.save(product);
                    log.info("Estoque restaurado: Produto {} (+{})", product.getName(), item.getQuantity());
                });
            } catch (Exception e) {
                log.error("Erro ao restaurar estoque do produto {}: {}", item.getProductId(), e.getMessage());
            }
        }
    }

    private void handleFinancialRefund(Appointment appt, ServiceProvider provider, boolean isClientAction) {
        if (!appt.isPaid() || appt.getExternalPaymentId() == null) return;

        boolean shouldRefund = !isClientAction || appt.isEligibleForRefund(provider.getCancellationMinHours());

        if (shouldRefund) {
            try {
                log.info("Iniciando estorno de R${}", appt.getFinalPrice());
                paymentProvider.refund(appt.getExternalPaymentId(), appt.getFinalPrice());
            } catch (Exception e) {
                log.error("FALHA CRÍTICA no estorno: {}", e.getMessage());
            }
        } else {
            log.warn("Cancelamento sem estorno (Regra de Multa/Prazo).");
        }
    }

    private void notifyParties(Appointment appt, String cancelledById) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
            String dateFormatted = appt.getStartTime().format(formatter);
            String serviceName = appt.getServices().isEmpty() ? "atendimento" : appt.getServices().get(0).getName();
            
            boolean isClientCancelled = cancelledById.equals(appt.getClientId());

            if (isClientCancelled) {
                String title = "⚠️ Agenda Liberada";
                String body = String.format("%s cancelou %s para %s.", appt.getClientName(), serviceName, dateFormatted);
                
                // Avisa Profissional e Dono
                Set<String> recipients = new HashSet<>();
                recipients.add(appt.getServiceProviderId());
                userRepository.findByProfessionalId(appt.getProfessionalId()).ifPresent(u -> recipients.add(u.getId()));

                for (String rid : recipients) notificationProvider.sendNotification(rid, title, body, "/dashboard/calendar");
            } else {
                String title = "❌ Agendamento Cancelado";
                String body = String.format("Seu horário de %s foi cancelado. Verifique o App.", dateFormatted);
                notificationProvider.sendNotification(appt.getClientId(), title, body, "/my-appointments");
            }
        } catch (Exception e) {
            log.error("Erro ao notificar cancelamento: {}", e.getMessage());
        }
    }

    public record CancelAppointmentInput(String appointmentId, String userId, String reason, boolean isClient) {}
}