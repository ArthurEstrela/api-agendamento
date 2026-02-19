package com.stylo.api_agendamento.adapters.outbound.events;

import com.stylo.api_agendamento.core.ports.IEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements IEventPublisher {

    private final ApplicationEventPublisher springPublisher;

    @Override
    public <T> void publish(T event) {
        springPublisher.publishEvent(event);
    }

    @Override
    public <T> void publishAll(List<T> events) {
        // Itera sobre a lista e publica cada evento individualmente
        if (events != null) {
            events.forEach(springPublisher::publishEvent);
        }
    }
}