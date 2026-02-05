package com.stylo.api_agendamento.core.ports;

import java.util.Optional;
import com.stylo.api_agendamento.core.domain.User;

public interface IUserRepository {
    Optional<User> findById(String id);
    Optional<User> findByEmail(String email);
    User save(User user);
    void updateProfile(User user);
    void delete(String id);
}