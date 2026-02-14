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

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Desabilita CSRF (API Stateless)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✨ Habilita CORS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // --- AUTENTICAÇÃO E REGISTRO ---
                        .requestMatchers(HttpMethod.POST, "/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/register/**").permitAll() // Cliente e afins

                        // --- RECUPERAÇÃO DE SENHA (✨ Novas Rotas) ---
                        .requestMatchers(HttpMethod.POST, "/v1/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/auth/reset-password").permitAll()

                        // --- CADASTRO DE ESTABELECIMENTO ---
                        .requestMatchers(HttpMethod.POST, "/v1/service-providers/register").permitAll()

                        // --- FLUXO PÚBLICO DE AGENDAMENTO (SaaS) ---
                        .requestMatchers(HttpMethod.GET, "/v1/service-providers/**").permitAll() // Buscar perfil pelo
                                                                                                 // slug
                        .requestMatchers(HttpMethod.GET, "/v1/appointments/slots").permitAll() // Ver horários livres
                        .requestMatchers(HttpMethod.GET, "/v1/services/**").permitAll() // Ver serviços
                        .requestMatchers(HttpMethod.GET, "/v1/products/**").permitAll() // Ver produtos (se público)
                        .requestMatchers(HttpMethod.GET, "/v1/reviews/**").permitAll() // Ver avaliações públicas

                        .requestMatchers(HttpMethod.PATCH, "/v1/appointments/*/no-show")
                        .hasAnyRole("PROFESSIONAL", "ADMIN")

                        // --- INTEGRATIONS & WEBHOOKS (Stripe, Google) ---
                        .requestMatchers("/v1/webhooks/**").permitAll()

                        // --- SEO ---
                        .requestMatchers(HttpMethod.GET, "/v1/sitemap.xml").permitAll()

                        // --- ÁREAS PROTEGIDAS ---
                        .requestMatchers("/v1/financial/**").hasRole("SERVICE_PROVIDER")
                        .requestMatchers("/v1/service-providers/settings/**").hasRole("SERVICE_PROVIDER")

                        // Qualquer outra requisição precisa de Token JWT
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