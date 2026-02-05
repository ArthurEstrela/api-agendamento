package com.stylo.api_agendamento.adapters.outbound.persistence;

import com.stylo.api_agendamento.adapters.outbound.persistence.mapper.UserMapper;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements IUserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(String id) {
        return jpaUserRepository.findById(id)
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity saved = jpaUserRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void updateProfile(User user) {
        if (!jpaUserRepository.existsById(user.getId())) {
            throw new EntityNotFoundException("Usuário não encontrado para atualização.");
        }
        jpaUserRepository.save(userMapper.toEntity(user));
    }

    @Override
    @Transactional
    public void delete(String id) {
        jpaUserRepository.deleteById(id);
    }
}