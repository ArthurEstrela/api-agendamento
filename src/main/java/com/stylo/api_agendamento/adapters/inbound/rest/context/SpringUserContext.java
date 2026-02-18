package com.stylo.api_agendamento.adapters.inbound.rest.context;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.ports.IUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringUserContext implements IUserContext {

    @Override
    public String getCurrentUserId() {
        return getUserPrincipal()
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Usuário não autenticado no contexto de segurança."));
    }

    @Override
    public String getCurrentUserEmail() {
        return getUserPrincipal()
                .map(User::getEmail)
                .orElseThrow(() -> new IllegalStateException("Usuário não autenticado."));
    }

    @Override
    public UserRole getCurrentUserRole() {
        return getUserPrincipal()
                .map(User::getRole)
                .orElse(UserRole.CLIENT);
    }

    @Override
    public boolean hasRole(UserRole role) {
        return getCurrentUserRole() == role;
    }

    @Override
    public Optional<String> getCurrentUserIdOptional() {
        return getUserPrincipal().map(User::getId);
    }

    // ✨ NOVO MÉTODO: Expõe o objeto User completo para acessar providerId, etc.
    public User getCurrentUser() {
        return getUserPrincipal()
                .orElseThrow(() -> new IllegalStateException("Usuário não autenticado ou contexto inválido."));
    }

    private Optional<User> getUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return Optional.of((User) principal);
        }

        return Optional.empty();
    }
}