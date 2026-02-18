package com.stylo.api_agendamento.adapters.inbound.rest.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import static com.stylo.api_agendamento.core.domain.UserPermission.*;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // --- PÚBLICO ---
                        .requestMatchers(HttpMethod.POST, "/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/service-providers/**").permitAll()
                        .requestMatchers("/v1/webhooks/**").permitAll()
                        // ... outros endpoints públicos ...

                        // --- FINANCEIRO (Proteção Crítica) ---
                        // Apenas quem tem FINANCIAL_READ vê o dashboard
                        .requestMatchers(HttpMethod.GET, "/v1/financial/**").hasAuthority(FINANCIAL_READ.getPermission())
                        // Apenas quem tem FINANCIAL_WRITE cria despesas ou saca (Dono)
                        .requestMatchers(HttpMethod.POST, "/v1/financial/**").hasAuthority(FINANCIAL_WRITE.getPermission())

                        // --- CONFIGURAÇÕES DO ESTABELECIMENTO ---
                        // Recepcionista não entra aqui, Gerente entra
                        .requestMatchers("/v1/service-providers/settings/**").hasAuthority(SETTINGS_WRITE.getPermission())

                        // --- GESTÃO DE SERVIÇOS E PRODUTOS ---
                        .requestMatchers(HttpMethod.POST, "/v1/services/**").hasAuthority(SETTINGS_WRITE.getPermission())
                        .requestMatchers(HttpMethod.POST, "/v1/products/**").hasAuthority(SETTINGS_WRITE.getPermission())

                        // --- AGENDAMENTOS ESPECIAIS ---
                        // Marcar No-Show: Precisa poder gerenciar agenda (Recepção, Gerente, Dono)
                        .requestMatchers(HttpMethod.PATCH, "/v1/appointments/*/no-show")
                            .hasAuthority(APPOINTMENT_MANAGE_ALL.getPermission())

                        // Qualquer outra requisição precisa apenas estar autenticada
                        // (o controle fino de "ver o próprio agendamento" vs "ver todos" 
                        // geralmente é feito dentro do Service/UseCase validando o ID)
                        .anyRequest().authenticated())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configuração de CORS para permitir que o Front-end (localhost ou produção)
     * acesse a API sem bloqueio do navegador.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Em produção, troque "*" pelo domínio do seu front (ex: https://stylo.app.br)
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}