package com.stylo.api_agendamento.adapters.outbound.notifications;

import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAdapter implements INotificationProvider {

    private final FcmNotificationAdapter fcmAdapter;
    private final ResendNotificationAdapter resendAdapter;
    private final IUserRepository userRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    @Override
    public void sendPushNotification(UUID userId, String title, String body, String actionUrl) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
                fcmAdapter.sendPush(user.getFcmToken(), title, body, actionUrl);
            }
        });
    }

    @Override
    public void sendAppointmentConfirmed(UUID userId, String clientName, String serviceName, LocalDateTime startTime) {
        String details = String.format("Olá %s, seu agendamento de %s para o dia %s foi confirmado!", 
                clientName, serviceName, startTime.format(DATE_FORMATTER));
        
        sendPushNotification(userId, "✅ Agendamento Confirmado!", details, "/appointments");
    }

    @Override
    public void sendAppointmentCancelled(UUID userId, String details, String reason) {
        String body = details + (reason != null ? "\nMotivo: " + reason : "");
        sendPushNotification(userId, "❌ Agendamento Cancelado", body, "/appointments");
    }

    @Override
    public void sendAppointmentRescheduled(UUID userId, String serviceName, LocalDateTime oldTime, LocalDateTime newTime) {
        String body = String.format("Seu serviço de %s foi movido de %s para %s", 
                serviceName, oldTime.format(DATE_FORMATTER), newTime.format(DATE_FORMATTER));
        
        sendPushNotification(userId, "🔄 Agendamento Reagendado", body, "/appointments");
    }

    @Override
    public void sendAppointmentReminder(String userId, String title, String body, String actionUrl, String email) {
        // Envia Push se houver ID de usuário
        if (userId != null) {
            sendPushNotification(UUID.fromString(userId), title, body, actionUrl);
        }
        
        // Envia E-mail de lembrete (Best effort)
        if (email != null && !email.isBlank()) {
            resendAdapter.sendEmail(email, title, body);
        }
    }

    @Override
    public void sendSystemAlert(String userId, String title, String body) {
        if (userId != null) {
            sendPushNotification(UUID.fromString(userId), "⚠️ Alerta Stylo: " + title, body, null);
        }
    }

    @Override
    public void sendWelcomeEmail(String email, String name) {
        resendAdapter.sendWelcomeEmail(email, name);
    }

    @Override
    public void sendPasswordResetEmail(String email, String name, String resetLink) {
        resendAdapter.sendPasswordResetEmailDirect(email, name, resetLink);
    }
}