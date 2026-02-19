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
        return () -> userContext.getCurrentUserIdOptional()
                .map(uuid -> uuid.toString()) // 1. Converte UUID para String (agora temos um Optional<String>)
                .or(() -> Optional.of("SYSTEM")); // 2. Agora o tipo coincide com Optional<String>
    }
}