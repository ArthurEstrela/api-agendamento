package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserPermission;
import com.stylo.api_agendamento.core.domain.UserRole;

import java.util.Optional;

public interface IUserContext {
    String getCurrentUserId();
    String getCurrentUserEmail();
    UserRole getCurrentUserRole();

    boolean hasRole(UserRole role);
    
    // ✨ NOVO: Permite checar permissões granulares direto na regra de negócio
    boolean hasPermission(UserPermission permission);

    Optional<String> getCurrentUserIdOptional();

    // ✨ NOVO: Expõe o usuário completo para acessarmos providerId, fcmToken, etc.
    // Isso evita que tenhamos que buscar o usuário no banco toda vez.
    User getCurrentUser();
}