package com.stylo.api_agendamento.config;

import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider") // ✨ Habilita a Auditoria
@RequiredArgsConstructor
public class AuditConfiguration {

    private final IUserContext userContext;

    @Bean
    public AuditorAware<String> auditorProvider() {
        // Retorna um lambda que busca o ID do usuário atual
        // Se não tiver usuário (ex: job agendado), retorna Empty ou "SYSTEM"
        return () -> userContext.getCurrentUserIdOptional()
                .or(() -> Optional.of("SYSTEM")); 
    }
}