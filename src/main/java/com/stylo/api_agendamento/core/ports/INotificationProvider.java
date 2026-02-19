package com.stylo.api_agendamento.core.ports;

import java.time.LocalDateTime;
import java.util.UUID;

public interface INotificationProvider {

    // --- AGENDAMENTOS ---
    void sendAppointmentConfirmed(UUID clientId, String clientName, String serviceName, LocalDateTime startTime);
    
    void sendAppointmentRescheduled(UUID clientId, String clientName, LocalDateTime oldTime, LocalDateTime newTime);
    
    void sendAppointmentCancelled(UUID clientId, String clientName, String reason);
    
    void sendAppointmentReminder(String toEmailOrPhone, String clientName, String businessName, String startTime);

    // --- PUSH NOTIFICATIONS (APP) ---
    void sendPushNotification(UUID userId, String title, String body, String actionUrl);

    // --- EMAILS TRANSACIONAIS ---
    void sendWelcomeEmail(String email, String name);
    
    void sendPasswordResetEmail(String email, String name, String resetLink);

    // --- INTERNO ---
    void sendSystemAlert(String adminEmail, String subject, String message);
}