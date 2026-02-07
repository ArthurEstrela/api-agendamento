package com.stylo.api_agendamento.core.ports;

public interface INotificationProvider {
    // Método para confirmação de agendamento
    void sendAppointmentConfirmed(String clientId, String message);
    
    // Método para reagendamento
    void sendAppointmentRescheduled(String clientId, String message);
    
    // Você também pode adicionar para cancelamentos futuramente
    void sendAppointmentCancelled(String clientId, String message);

    void sendAppointmentReminder(String clientId, String message);
}