package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SendRemindersUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final INotificationProvider notificationProvider;
    private final IUserRepository userRepository;

    @Transactional
    public void execute() {
        LocalDateTime now = LocalDateTime.now();
        
        // Busca agendamentos SCHEDULED que atingiram a janela de lembrete
        List<Appointment> pendingReminders = appointmentRepository.findPendingReminders(now);

        if (pendingReminders.isEmpty()) return;

        log.info("⏰ Processando {} lembretes de agendamento.", pendingReminders.size());

        for (Appointment appt : pendingReminders) {
            try {
                dispatchNotification(appt);
                
                // Marca como enviado para garantir idempotência
                appt.markReminderAsSent();
                appointmentRepository.save(appt);
                
            } catch (Exception e) {
                log.error("❌ Falha ao processar lembrete do agendamento {}: {}", appt.getId(), e.getMessage());
            }
        }
    }

    private void dispatchNotification(Appointment appt) {
        String timeStr = appt.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String services = appt.getServicesSnapshot(); // Método no domínio que junta os nomes
        
        // Prioridade 1: Push Notification (Mobile Engagement)
        userRepository.findById(appt.getClientId()).ifPresent(user -> {
            if (user.getFcmToken() != null) {
                notificationProvider.sendPushNotification(
                    user.getId(),
                    "🔔 Lembrete Stylo",
                    String.format("Olá %s! Você tem um horário hoje às %s para [%s].", appt.getClientName(), timeStr, services),
                    "/appointments/" + appt.getId()
                );
            }
        });

        // Prioridade 2: E-mail (Registro e Fallback)
        notificationProvider.sendAppointmentReminder(
                appt.getClientEmail(),
                appt.getClientName(),
                appt.getBusinessName(),
                timeStr,
                services
        );
    }
}