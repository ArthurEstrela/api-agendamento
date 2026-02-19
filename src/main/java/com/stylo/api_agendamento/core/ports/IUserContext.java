package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserPermission;
import com.stylo.api_agendamento.core.domain.UserRole;

import java.util.Optional;
import java.util.UUID;

public interface IUserContext {
    
    /**
     * Retorna o ID do usuário logado. 
     * Lança exceção se não houver usuário autenticado.
     */
    UUID getCurrentUserId();

    /**
     * Retorna o ID de forma segura (sem exceção).
     */
    Optional<UUID> getCurrentUserIdOptional();

    String getCurrentUserEmail();
    
    UserRole getCurrentUserRole();

    boolean hasRole(UserRole role);
    
    boolean hasPermission(UserPermission permission);

    /**
     * Retorna a entidade completa do usuário logado.
     * Útil para acessar dados como providerId, clientId, nome, etc. sem ir ao repositório novamente.
     */
    User getCurrentUser();
}