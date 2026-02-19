package com.stylo.api_agendamento.adapters.outbound.persistence.user;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements IUserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        var entity = userMapper.toEntity(user);
        var savedEntity = jpaUserRepository.save(entity);
        return userMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaUserRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    // ✨ NOVO MÉTODO IMPLEMENTADO
    @Override
    public Optional<User> findByProfessionalId(UUID professionalId) {
        return jpaUserRepository.findByProfessionalId(professionalId)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByProviderId(UUID providerId) {
        return jpaUserRepository.findByProviderId(providerId)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByResetPasswordToken(String token) {
        return jpaUserRepository.findByResetPasswordToken(token)
                .map(userMapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        jpaUserRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void clearTokenIfInUse(String fcmToken) {
        if (fcmToken != null && !fcmToken.isBlank()) {
            jpaUserRepository.clearFcmToken(fcmToken);
        }
    }
}