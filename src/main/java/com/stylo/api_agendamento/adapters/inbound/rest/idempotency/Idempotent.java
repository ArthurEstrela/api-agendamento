package com.stylo.api_agendamento.adapters.inbound.rest.idempotency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    // Tempo que a resposta ficará salva no cache (ex: 24 horas)
    long ttl() default 24;
    
    // Unidade de tempo
    TimeUnit unit() default TimeUnit.HOURS;
}