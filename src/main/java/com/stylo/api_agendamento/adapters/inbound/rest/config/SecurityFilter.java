package com.stylo.api_agendamento.adapters.inbound.rest.config;

import com.google.firebase.auth.FirebaseToken;
import com.stylo.api_agendamento.adapters.outbound.security.TokenService;
import com.stylo.api_agendamento.core.domain.User;
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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final IUserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui") || path.equals("/v1/payments/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = this.recoverToken(request);
        
        if (token != null) {
            FirebaseToken firebaseToken = tokenService.validateToken(token);
            
            if (firebaseToken != null) {
                String uid = firebaseToken.getUid();
                String email = firebaseToken.getEmail();

                // ✨ 1. Tenta buscar PRIMEIRO pelo ID do Firebase (O mais seguro)
                Optional<User> userOpt = userRepository.findByFirebaseId(uid);

                // ✨ 2. Fallback: Se não achou pelo UID, tenta pelo E-mail (Retrocompatibilidade)
                if (userOpt.isEmpty() && email != null && !email.isBlank()) {
                    userOpt = userRepository.findByEmail(email);
                    
                    // ✨ 3. Auto-cura (Self-Healing): Achou pelo e-mail? Vincula o UID para as próximas vezes!
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        user.linkFirebase(uid);
                        userRepository.save(user); // Salva o vínculo definitivo
                        logger.info("Vínculo Firebase UID realizado com sucesso para o usuário: {}", email);
                    }
                }

                userOpt.ifPresentOrElse(
                    user -> {
                        // ✨ 4. Autentica no contexto do Spring Security com as Roles corretas
                        var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    },
                    () -> logger.warn("Aviso: Token Firebase válido, mas usuário não encontrado no banco local (UID: {}, E-mail: {})", uid, email)
                );
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}