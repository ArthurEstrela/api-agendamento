package com.stylo.api_agendamento.adapters.outbound.events;

import com.stylo.api_agendamento.core.ports.IEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringEventPublisherAdapter implements IEventPublisher {

    private final ApplicationEventPublisher springPublisher;

    @Override
    public void publish(Object event) {
        springPublisher.publishEvent(event);
    }
}