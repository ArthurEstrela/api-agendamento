package com.stylo.api_agendamento.adapters.inbound.rest.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

import com.stylo.api_agendamento.core.domain.UserPermission;

import static com.stylo.api_agendamento.core.domain.UserPermission.*;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    // ✨ INJEÇÃO DA URL DO FRONT-END (Vem do application.properties ou Variável de Ambiente)
    @Value("${stylo.frontend-url}")
    private String frontendUrl;

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
                        // Rota exata do webhook do Stripe mapeada no PaymentController
                        .requestMatchers(HttpMethod.POST, "/v1/payments/webhook").permitAll() 
                        // Documentação do Swagger/OpenAPI
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // --- FINANCEIRO (Proteção Crítica) ---
                        // Apenas quem tem FINANCIAL_READ vê o dashboard
                        .requestMatchers(HttpMethod.GET, "/v1/financial/**")
                        .hasAuthority(FINANCIAL_READ.getPermission())
                        // Apenas quem tem FINANCIAL_WRITE cria despesas ou saca (Dono)
                        .requestMatchers(HttpMethod.POST, "/v1/financial/**")
                        .hasAuthority(FINANCIAL_WRITE.getPermission())

                        // --- CONFIGURAÇÕES DO ESTABELECIMENTO ---
                        // Recepcionista não entra aqui, Gerente entra
                        .requestMatchers("/v1/service-providers/settings/**")
                        .hasAuthority(SETTINGS_WRITE.getPermission())

                        // --- GESTÃO DE SERVIÇOS E PRODUTOS ---
                        .requestMatchers(HttpMethod.POST, "/v1/services/**")
                        .hasAuthority(SETTINGS_WRITE.getPermission())
                        .requestMatchers(HttpMethod.POST, "/v1/products/**")
                        .hasAuthority(SETTINGS_WRITE.getPermission())

                        // --- CUPONS ---
                        // Criar cupons: Requer permissão de escrita em configurações ou financeira
                        .requestMatchers(HttpMethod.POST, "/v1/coupons").hasAnyAuthority(
                                UserPermission.SETTINGS_WRITE.getPermission(),
                                UserPermission.FINANCIAL_WRITE.getPermission())
                        // Validar cupom: Aberto para autenticados (Clientes usando o app)
                        .requestMatchers(HttpMethod.GET, "/v1/coupons/validate").authenticated()

                        .requestMatchers("/v1/pos/**").hasAnyAuthority(
                                UserPermission.APPOINTMENT_MANAGE_ALL.getPermission(), // Recepcionista
                                UserPermission.APPOINTMENT_WRITE.getPermission() // Profissional (para fechar a própria conta)
                        )

                        // --- AGENDAMENTOS ESPECIAIS ---
                        // Marcar No-Show: Precisa poder gerenciar agenda (Recepção, Gerente, Dono)
                        .requestMatchers(HttpMethod.PATCH, "/v1/appointments/*/no-show")
                        .hasAuthority(APPOINTMENT_MANAGE_ALL.getPermission())

                        .requestMatchers("/v1/financial/cash-register/**")
                        .hasAuthority(UserPermission.FINANCIAL_WRITE.getPermission())
                        
                        // Qualquer outra requisição precisa apenas estar autenticada
                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configuração de CORS blindada para produção.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Permite APENAS o domínio seguro do SaaS (ou localhost durante o dev)
        configuration.setAllowedOrigins(List.of(frontendUrl));
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Incluído Stripe-Signature para garantir o tráfego do Webhook
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Stripe-Signature"));
        
        // Essencial para frameworks modernos de front-end
        configuration.setAllowCredentials(true);

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