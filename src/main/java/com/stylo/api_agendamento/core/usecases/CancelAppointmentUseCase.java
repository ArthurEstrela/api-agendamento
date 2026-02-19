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
    private final IStockMovementRepository stockMovementRepository; // ✨ Para auditoria de devolução
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

        // 2. Busca estabelecimento para validar regras de cancelamento e fuso horário
        ServiceProvider provider = providerRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 3. Devolução de Estoque com Registro de Movimentação (Kardex)
        returnProductsToStock(appointment, input.userId());

        // 4. Processamento de Estorno Financeiro
        handleFinancialRefund(appointment, provider, input.isClient());

        // 5. Atualização de Estado no Domínio
        appointment.cancel();
        appointment.setCancellationReason(input.reason());
        appointment.setCancelledBy(input.userId().toString());
        
        // 6. Persistência
        appointmentRepository.save(appointment);
        
        // 7. Sincronização de Calendário Externo (Silent failure para não travar o cancelamento)
        if (appointment.getExternalEventId() != null) {
            try {
                calendarProvider.deleteEvent(appointment.getProfessionalId(), appointment.getExternalEventId());
            } catch (Exception e) {
                log.warn("Falha ao remover evento do calendário externo: {}", e.getMessage());
            }
        }

        // 8. Disparo de Notificações Push/Email
        notifyParties(appointment, input.userId());

        // 9. Publicação de Evento para Waitlist (Gatilho para preencher a vaga liberada)
        eventPublisher.publish(new AppointmentCancelledEvent(
                appointment.getId(),
                appointment.getProfessionalId(),
                appointment.getClientId(),
                appointment.getClientName(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                input.reason()
        ));
        
        log.info("Agendamento {} cancelado. Motivo: {}", appointment.getId(), input.reason());
    }

    /**
     * Devolve os produtos ao estoque e gera movimentação auditável.
     */
    private void returnProductsToStock(Appointment appointment, UUID operatorId) {
        if (!appointment.hasProducts()) return;

        for (Appointment.AppointmentItem item : appointment.getProducts()) {
            productRepository.findById(item.getProductId()).ifPresent(product -> {
                // 1. Incrementa a quantidade no produto
                product.addStock(item.getQuantity());
                productRepository.save(product);

                // 2. Registra o movimento de entrada (Kardex)
                StockMovement movement = StockMovement.create(
                        product.getId(),
                        product.getServiceProviderId(),
                        StockMovementType.RETURN_FROM_CUSTOMER, // Tipo: Devolução
                        item.getQuantity(),
                        "Devolução por cancelamento do agendamento #" + appointment.getId().toString().substring(0, 8),
                        operatorId
                );
                stockMovementRepository.save(movement);
                
                log.info("Estoque restaurado: {} (+{})", product.getName(), item.getQuantity());
            });
        }
    }

    /**
     * Gerencia a devolução do dinheiro baseada na política de cancelamento do salão.
     */
    private void handleFinancialRefund(Appointment appt, ServiceProvider provider, boolean isClientAction) {
        if (!appt.isPaid() || appt.getExternalPaymentId() == null) return;

        // Regra: Se o profissional cancelar, o estorno é SEMPRE total.
        // Se o cliente cancelar, verifica se ele está dentro do prazo (ex: 2h antes).
        boolean shouldRefund = !isClientAction || appt.isEligibleForRefund(provider.getCancellationMinHours());

        if (shouldRefund) {
            try {
                log.info("Processando estorno automático de R$ {} para o agendamento {}", appt.getFinalPrice(), appt.getId());
                paymentProvider.refundPayment(appt.getExternalPaymentId(), appt.getFinalPrice());
            } catch (Exception e) {
                log.error("ERRO CRÍTICO no estorno (ID PGTO: {}): {}", appt.getExternalPaymentId(), e.getMessage());
                // Aqui poderíamos criar um alerta para o administrador do sistema
            }
        } else {
            log.warn("Cancelamento fora do prazo. Estorno não realizado conforme política do salão.");
        }
    }

    /**
     * Notifica as partes envolvidas via Push/App.
     */
    private void notifyParties(Appointment appt, UUID operatorId) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
            String dateFormatted = appt.getStartTime().format(formatter);
            
            // Se quem cancelou foi o cliente
            if (operatorId.equals(appt.getClientId())) {
                String title = "⚠️ Vaga Liberada";
                String body = String.format("%s cancelou o horário de %s.", appt.getClientName(), dateFormatted);
                
                // Notifica o dono (Provider)
                notificationProvider.sendPushNotification(appt.getServiceProviderId(), title, body, "/admin/calendar");
                
                // Notifica o profissional específico
                userRepository.findByProviderId(appt.getProfessionalId()) // Assume que o profissional tem um User vinculado
                        .ifPresent(u -> notificationProvider.sendPushNotification(u.getId(), title, body, "/pro/calendar"));
            } 
            // Se quem cancelou foi o profissional/salão
            else {
                String title = "❌ Agendamento Cancelado";
                String body = String.format("Seu horário de %s foi cancelado pelo estabelecimento.", dateFormatted);
                notificationProvider.sendPushNotification(appt.getClientId(), title, body, "/client/appointments");
            }
        } catch (Exception e) {
            log.warn("Erro ao enviar notificações de cancelamento: {}", e.getMessage());
        }
    }

    public record Input(
            UUID appointmentId,
            UUID userId,
            String reason,
            boolean isClient
    ) {}
}