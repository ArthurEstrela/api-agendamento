package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface IUserRepository {
    
    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    /**
     * Verifica existência (mais leve que carregar a entidade inteira).
     */
    boolean existsByEmail(String email);

    /**
     * Busca usuário vinculado a um perfil profissional específico.
     */
    Optional<User> findByProviderId(UUID providerId);

    Optional<User> findByResetPasswordToken(String token);
    
    void delete(UUID id);
}