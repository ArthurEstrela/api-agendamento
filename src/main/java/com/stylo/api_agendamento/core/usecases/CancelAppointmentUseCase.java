package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.events.AppointmentCancelledEvent;
import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import com.stylo.api_agendamento.core.domain.stock.StockMovementType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IServiceProviderRepository providerRepository;
    private final IUserRepository userRepository;
    private final IProductRepository productRepository;
    private final IStockMovementRepository stockMovementRepository;
    private final INotificationProvider notificationProvider;
    private final IPaymentProvider paymentProvider;
    private final ICalendarProvider calendarProvider;
    private final IEventPublisher eventPublisher;

    @Transactional
    public void execute(Input input) {
        // 1. Busca e validação inicial
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Este agendamento já se encontra cancelado.");
        }

        // 2. Busca estabelecimento para validar regras de política de cancelamento
        ServiceProvider provider = providerRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 3. Devolução de Produtos ao Estoque com Registro Auditável
        returnProductsToStock(appointment, input.userId());

        // 4. Processamento de Estorno Financeiro (Baseado na política do salão)
        handleFinancialRefund(appointment, provider, input.isClient());

        // 5. Atualização de Estado no Domínio
        appointment.cancel();
        appointment.setCancellationReason(input.reason());
        appointment.setCancelledBy(input.userId().toString());
        
        // 6. Persistência da Alteração
        appointmentRepository.save(appointment);
        
        // 7. Sincronização de Calendário Externo (Google/Outlook)
        if (appointment.getExternalEventId() != null) {
            try {
                // ✨ CORREÇÃO: Ordem dos parâmetros corrigida para (String, UUID)
                calendarProvider.deleteEvent(appointment.getExternalEventId(), appointment.getProfessionalId());
            } catch (Exception e) {
                log.warn("Falha ao remover evento do calendário externo: {}", e.getMessage());
            }
        }

        // 8. Disparo de Notificações Push
        notifyParties(appointment, input.userId());

        // 9. Publicação de Evento para Fila de Espera (Waitlist)
        eventPublisher.publish(new AppointmentCancelledEvent(
                appointment.getId(),
                appointment.getProfessionalId(),
                appointment.getClientId(),
                appointment.getClientName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                input.reason()
        ));
        
        log.info("Agendamento {} cancelado com sucesso. Operador: {}", appointment.getId(), input.userId());
    }

    private void returnProductsToStock(Appointment appointment, UUID operatorId) {
        if (!appointment.hasProducts()) return;

        for (Appointment.AppointmentItem item : appointment.getProducts()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                product.addStock(item.getQuantity());
                productRepository.save(product);

                // Registra o movimento de entrada por devolução
                StockMovement movement = StockMovement.create(
                        product.getId(),
                        product.getServiceProviderId(),
                        StockMovementType.RETURN_FROM_CUSTOMER,
                        item.getQuantity(),
                        "Devolução por cancelamento do agendamento #" + appointment.getId().toString().substring(0, 8),
                        operatorId
                );
                stockMovementRepository.save(movement);
            });
        }
    }

    private void handleFinancialRefund(Appointment appt, ServiceProvider provider, boolean isClientAction) {
        if (!appt.isPaid() || appt.getExternalPaymentId() == null) return;

        // Regra: Estorno total se estabelecimento cancelar ou se cliente estiver no prazo
        boolean shouldRefund = !isClientAction || appt.isEligibleForRefund(provider.getCancellationMinHours());

        if (shouldRefund) {
            try {
                log.info("Processando estorno de R$ {} para o agendamento {}", appt.getFinalPrice(), appt.getId());
                paymentProvider.refundPayment(appt.getExternalPaymentId(), appt.getFinalPrice());
            } catch (Exception e) {
                log.error("FALHA NO ESTORNO (ID: {}): {}", appt.getExternalPaymentId(), e.getMessage());
            }
        }
    }

    private void notifyParties(Appointment appt, UUID operatorId) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
            String dateFormatted = appt.getStartTime().format(formatter);
            
            // Se o CLIENTE cancelou -> Notifica Profissional e Estabelecimento
            if (operatorId.equals(appt.getClientId())) {
                String title = "⚠️ Vaga Liberada";
                String body = String.format("%s cancelou o horário de %s.", appt.getClientName(), dateFormatted);
                
                // Notifica o dono/gestor
                userRepository.findByProviderId(appt.getServiceProviderId())
                        .ifPresent(u -> notificationProvider.sendPushNotification(u.getId(), title, body, "/admin/calendar"));
                
                // Notifica o profissional específico
                userRepository.findByProfessionalId(appt.getProfessionalId())
                        .ifPresent(u -> notificationProvider.sendPushNotification(u.getId(), title, body, "/pro/calendar"));
            } 
            // Se o ESTABELECIMENTO cancelou -> Notifica Cliente
            else {
                String title = "❌ Agendamento Cancelado";
                String body = String.format("Seu horário de %s foi cancelado pelo estabelecimento.", dateFormatted);
                
                if (appt.getClientId() != null) {
                    notificationProvider.sendPushNotification(appt.getClientId(), title, body, "/client/appointments");
                }
            }
        } catch (Exception e) {
            log.warn("Erro ao enviar notificações de cancelamento: {}", e.getMessage());
        }
    }

    public record Input(UUID appointmentId, UUID userId, String reason, boolean isClient) {}
}