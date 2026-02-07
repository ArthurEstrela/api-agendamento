package com.stylo.api_agendamento.adapters.outbound.persistence.user;

import com.stylo.api_agendamento.adapters.outbound.persistence.user.UserMapper;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public void updateProfile(User user) {
        // No JPA, o save() faz o merge se o ID já existir
        jpaUserRepository.save(userMapper.toEntity(user));
    }

    @Override
    public void delete(String id) {
        jpaUserRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findById(String id) {
        return jpaUserRepository.findById(UUID.fromString(id))
                .map(userMapper::toDomain);
    }
}