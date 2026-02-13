package com.stylo.api_agendamento.adapters.outbound.notifications;

import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAdapter implements INotificationProvider {

    private final FcmNotificationAdapter fcmAdapter;
    private final ResendNotificationAdapter resendAdapter; // ✨ Agora chama a classe concreta
    private final IUserRepository userRepository;

    @Override
    public void sendNotification(String userId, String title, String body) {
        sendNotification(userId, title, body, null);
    }

    @Override
    public void sendNotification(String userId, String title, String body, String actionUrl) {
        userRepository.findById(userId).ifPresent(user -> {
            // 1. Push
            if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
                fcmAdapter.sendPush(user.getFcmToken(), title, body, actionUrl);
            }
            // 2. Email (se quiser mandar email para toda notificação, descomente abaixo)
            // resendAdapter.sendEmail(user.getEmail(), title, body); 
        });
    }

    @Override
    public void sendAppointmentConfirmed(String userId, String details) {
        sendNotification(userId, "✅ Agendamento Confirmado!", details);
    }

    @Override
    public void sendAppointmentCancelled(String userId, String details) {
        sendNotification(userId, "❌ Agendamento Cancelado", details);
    }

    @Override
    public void sendAppointmentRescheduled(String userId, String details) {
        sendNotification(userId, "🔄 Agendamento Reagendado", details);
    }

    @Override
    public void sendAppointmentReminder(String userId, String title, String body, String actionUrl) {
        sendNotification(userId, title, body, actionUrl);
        // Opcional: Mandar e-mail de lembrete também
        userRepository.findById(userId).ifPresent(u -> 
            resendAdapter.sendEmail(u.getEmail(), title, body)
        );
    }

    @Override
    public void sendWelcomeEmail(String email, String name) {
        // ✨ Chama o método específico do especialista em e-mail
        resendAdapter.sendWelcomeEmail(email, name);
    }
}