package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.events.AppointmentCancelledEvent; // ✨ Import novo
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
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
    private final INotificationProvider notificationProvider;
    private final IPaymentProvider paymentProvider;
    private final ICalendarProvider calendarProvider; // ✨ Necessário para remover do Google
    private final IEventPublisher eventPublisher;     // ✨ Necessário para avisar a Waitlist

    public void execute(CancelAppointmentInput input) {
        // 1. Busca e validação básica
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Agendamento já está cancelado.");
        }

        // 2. Busca o estabelecimento para validar políticas de cancelamento
        ServiceProvider provider = providerRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new BusinessException("Estabelecimento não encontrado."));

        // 3. Processamento Financeiro (Regra de Estorno Inteligente)
        handleFinancialRefund(appointment, provider, input.isClient());

        // 4. Executa o cancelamento no domínio
        appointment.cancel();
        appointment.setCancellationReason(input.reason());
        appointment.setCancelledBy(input.userId());
        
        // 5. Persistência
        appointmentRepository.save(appointment);
        
        // 6. Remove do Google Calendar (Síncrono ou Assíncrono, aqui mantemos direto para garantir)
        if (appointment.getExternalEventId() != null) {
            calendarProvider.deleteEvent(appointment.getProfessionalId(), appointment.getExternalEventId());
        }

        log.info("✅ Agendamento {} cancelado com sucesso por {}.", appointment.getId(), input.userId());

        // 7. Notificação Cruzada (Partes envolvidas)
        notifyParties(appointment, input.userId());

        // 8. ✨ DISPARO DE EVENTO PARA WAITLIST
        // Isso ativa o WaitlistListener em segundo plano
        eventPublisher.publish(new AppointmentCancelledEvent(
                appointment.getId(),
                appointment.getProfessionalId(),
                appointment.getStartTime(),
                appointment.getEndTime()
        ));
    }

    private void handleFinancialRefund(Appointment appt, ServiceProvider provider, boolean isClientAction) {
        if (!appt.isPaid() || appt.getExternalPaymentId() == null) return;

        boolean shouldRefund = !isClientAction || appt.isEligibleForRefund(provider.getCancellationMinHours());

        if (shouldRefund) {
            try {
                log.info("💸 Iniciando estorno de R${} para o agendamento {}", appt.getFinalPrice(), appt.getId());
                paymentProvider.refund(appt.getExternalPaymentId(), appt.getFinalPrice());
            } catch (Exception e) {
                log.error("🔥 FALHA CRÍTICA no estorno do agendamento {}: {}", appt.getId(), e.getMessage());
            }
        } else {
            log.warn("⚠️ Cancelamento tardio pelo cliente {}. Valor retido como multa.", appt.getClientName());
        }
    }

    private void notifyParties(Appointment appt, String cancelledById) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
            String dateFormatted = appt.getStartTime().format(formatter);
            String serviceName = appt.getServices().isEmpty() ? "atendimento" : appt.getServices().get(0).getName();
            
            boolean isClientCancelled = cancelledById.equals(appt.getClientId());

            if (isClientCancelled) {
                String title = "⚠️ Agenda Liberada - Cancelamento";
                String body = String.format("%s cancelou o horário de %s em %s.", appt.getClientName(), serviceName, dateFormatted);

                Set<String> recipients = new HashSet<>();
                recipients.add(appt.getServiceProviderId());
                userRepository.findByProfessionalId(appt.getProfessionalId()).ifPresent(u -> recipients.add(u.getId()));

                for (String rid : recipients) {
                    notificationProvider.sendNotification(rid, title, body, "/dashboard/calendar");
                }
            } else {
                String title = "❌ Agendamento Cancelado";
                String body = String.format("Seu horário de %s em %s foi cancelado pelo estabelecimento. Verifique seu e-mail para detalhes.", serviceName, dateFormatted);
                notificationProvider.sendNotification(appt.getClientId(), title, body, "/my-appointments");
            }
        } catch (Exception e) {
            log.error("Erro ao processar notificações de cancelamento: {}", e.getMessage());
        }
    }

    public record CancelAppointmentInput(String appointmentId, String userId, String reason, boolean isClient) {}
}