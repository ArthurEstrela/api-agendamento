package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ConfirmAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final INotificationProvider notificationProvider;

    @Transactional
    public void execute(Input input) {
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // ✨ Segurança: Tenant Isolation
        if (!appointment.getServiceProviderId().equals(input.providerId())) {
            throw new BusinessException("Acesso negado: Este agendamento não pertence ao seu estabelecimento.");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            log.warn("Tentativa de confirmar agendamento {} que já está em status {}", appointment.getId(), appointment.getStatus());
            throw new BusinessException("Apenas agendamentos pendentes podem ser confirmados.");
        }

        // Mudança de Estado
        appointment.confirm(); // Método rico no domínio (set status SCHEDULED)
        appointmentRepository.save(appointment);

        log.info("Agendamento {} confirmado pelo estabelecimento {}.", appointment.getId(), input.providerId());

        // Notificação rica ao cliente
        notifyClient(appointment);
    }

    private void notifyClient(Appointment appt) {
        try {
            String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"));
            String serviceName = appt.getServices().isEmpty() ? "seu horário" : appt.getServices().get(0).getName();
            
            notificationProvider.sendPushNotification(
                appt.getClientId(), 
                "✅ Agendamento Confirmado!", 
                String.format("Tudo certo! Seu horário para %s em %s foi confirmado.", serviceName, dateFormatted),
                "/client/appointments/" + appt.getId()
            );
        } catch (Exception e) {
            log.error("Erro ao notificar cliente da confirmação: {}", e.getMessage());
        }
    }

    public record Input(UUID appointmentId, UUID providerId) {}
}