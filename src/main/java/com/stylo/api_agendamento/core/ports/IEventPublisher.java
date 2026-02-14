package com.stylo.api_agendamento.core.ports;

public interface IEventPublisher {
    void publish(Object event);
}