package com.stylo.api_agendamento.adapters.inbound.rest.config;

import com.stylo.api_agendamento.adapters.outbound.security.TokenService;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final IUserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        // ✨ Otimização: Pula a verificação de token para rotas públicas como Swagger ou Webhooks
        String path = request.getRequestURI();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/v1/payments/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = this.recoverToken(request);
        
        if (token != null) {
            var email = tokenService.validateToken(token);
            
            if (email != null && !email.isBlank()) {
                // Busca o usuário no banco local pelo e-mail verificado pelo Firebase
                userRepository.findByEmail(email).ifPresentOrElse(
                    user -> {
                        // Usuário encontrado: Autentica no contexto do Spring Security
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    },
                    () -> {
                        // Usuário logado no Firebase, mas não existe no BD Postgres.
                        // O Spring Security irá retornar 403/401 automaticamente dependendo da rota.
                        logger.warn("Aviso: Token Firebase válido, mas usuário não encontrado no banco local (e-mail: {})", email);
                    }
                );
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        
        // Verifica se o cabeçalho existe e começa com o padrão correto
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        // Retorna apenas o token usando substring (mais eficiente e seguro que o replace)
        return authHeader.substring(7);
    }
}