package com.stylo.api_agendamento.core.ports;

public interface INotificationProvider {
    void send(String userId, String title, String message);
}