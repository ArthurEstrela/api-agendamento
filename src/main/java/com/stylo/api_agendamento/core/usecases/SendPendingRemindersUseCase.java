package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SendPendingRemindersUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final INotificationProvider notificationProvider;
    private final IUserRepository userRepository;

    public void execute() {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Busca agendamentos que atingiram o tempo de lembrete (startTime - reminderMinutes <= now)
        // A lógica de filtragem por tempo é feita via Query para performance.
        List<Appointment> pendingAppointments = appointmentRepository.findPendingReminders(now);

        if (pendingAppointments.isEmpty()) return;

        log.info("⏰ Processando {} lembretes pendentes.", pendingAppointments.size());

        for (Appointment appt : pendingAppointments) {
            try {
                // 2. Dispara Notificação Multi-canal (Push + Email)
                sendNotifications(appt);

                // 3. Marca como enviado no domínio e persiste
                appt.markReminderAsSent();
                appointmentRepository.save(appt);
                
                log.info("✅ Lembrete enviado para o cliente: {}", appt.getClientName());
            } catch (Exception e) {
                log.error("❌ Falha ao processar lembrete {}: {}", appt.getId(), e.getMessage());
            }
        }
    }

    private void sendNotifications(Appointment appt) {
        String timeFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM"));
        
        String title = "🔔 Lembrete de Agendamento";
        String body = String.format("Olá %s, você tem um horário hoje às %s com %s.", 
                appt.getClientName(), timeFormatted, appt.getBusinessName());

        // A. Canal: E-mail (Via Resend/SMTP)
        notificationProvider.sendAppointmentReminder(
                appt.getClientEmail(),
                appt.getClientName(),
                appt.getBusinessName(),
                dateFormatted + " às " + timeFormatted
        );

        // B. Canal: Push Notification (Via FCM - se o cliente tiver Token)
        if (appt.getClientId() != null) {
            userRepository.findById(appt.getClientId()).ifPresent(user -> {
                if (user.getFcmToken() != null) {
                    notificationProvider.sendNotification(user.getId(), title, body, "/appointments");
                }
            });
        }
    }
}