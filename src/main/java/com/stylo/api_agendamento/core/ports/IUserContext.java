package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.UserRole;
import java.util.Optional;

public interface IUserContext {
    /**
     * Retorna o ID do usuário autenticado na requisição atual.
     * @throws IllegalStateException se nenhum usuário estiver logado.
     */
    String getCurrentUserId();

    /**
     * Retorna o Email do usuário autenticado.
     */
    String getCurrentUserEmail();

    /**
     * Retorna a Role (Perfil) do usuário autenticado.
     */
    UserRole getCurrentUserRole();

    /**
     * Verifica se o usuário tem um perfil específico.
     */
    boolean hasRole(UserRole role);

    /**
     * Retorna o ID do usuário de forma segura (Optional), útil para rotas públicas.
     */
    Optional<String> getCurrentUserIdOptional();
}