package com.stylo.api_agendamento.core.ports;

public interface INotificationProvider {
    void sendNotification(String userId, String message);
}