package com.stylo.api_agendamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

import com.stylo.api_agendamento.core.common.UseCase;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = "com.stylo.api_agendamento", includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = UseCase.class))
@EnableCaching
public class ApiAgendamentoApplication {

    @PostConstruct
    public void init() {
        // ✨ CORREÇÃO CRÍTICA: Trava o fuso horário da aplicação inteira em UTC.
        // Isso garante que todo LocalDateTime armazenado no banco represente o tempo universal.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ApiAgendamentoApplication.class, args);
    }
}