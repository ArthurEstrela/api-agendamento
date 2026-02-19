package com.stylo.api_agendamento.adapters.inbound.rest.context;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserPermission;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.ports.IUserContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SpringUserContext implements IUserContext {

    @Override
    public UUID getCurrentUserId() { // ✨ Corrigido: tipo deve ser UUID
        return getUserPrincipal()
                .map(User::getId) // User::getId retorna UUID
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
    public boolean hasPermission(UserPermission permission) {
        return getCurrentUserRole().getPermissions().contains(permission);
    }

    @Override
    public Optional<UUID> getCurrentUserIdOptional() { // ✨ Corrigido: tipo deve ser Optional<UUID>
        return getUserPrincipal().map(User::getId);
    }

    @Override
    public User getCurrentUser() {
        return getUserPrincipal()
                .orElseThrow(() -> new IllegalStateException("Usuário não autenticado ou contexto inválido."));
    }

    private Optional<User> getUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return Optional.of((User) principal);
        }

        return Optional.empty();
    }
}