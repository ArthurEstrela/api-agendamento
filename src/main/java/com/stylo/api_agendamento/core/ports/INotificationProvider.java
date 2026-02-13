package com.stylo.api_agendamento.core.ports;

public interface INotificationProvider {
    // Método para confirmação de agendamento
    void sendAppointmentConfirmed(String clientId, String message);

    // Método para reagendamento
    void sendAppointmentRescheduled(String clientId, String message);

    // Você também pode adicionar para cancelamentos futuramente
    void sendAppointmentCancelled(String clientId, String message);

    void sendAppointmentReminder(String to, String clientName, String businessName, String startTime);

    void sendNotification(String userId, String title, String body);

    void sendNotification(String userId, String title, String body, String actionUrl);

    void sendWelcomeEmail(String email, String name);
}