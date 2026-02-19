package com.stylo.api_agendamento.core.ports;

import java.util.List;

public interface IEventPublisher {
    
    /**
     * Publica um evento de domínio único.
     */
    <T> void publish(T event);

    /**
     * Publica uma lista de eventos em lote (útil para performance).
     */
    <T> void publishAll(List<T> events);
}