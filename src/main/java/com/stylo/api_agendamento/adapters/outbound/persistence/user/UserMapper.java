package com.stylo.api_agendamento.adapters.outbound.persistence.user;

import com.stylo.api_agendamento.core.domain.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(User domain) {
        return UserEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .email(domain.getEmail())
                .password(domain.getPassword()) // Mapeando senha
                .role(domain.getRole())
                .phoneNumber(domain.getPhoneNumber())
                .profilePictureUrl(domain.getProfilePictureUrl())
                .createdAt(domain.getCreatedAt())
                .active(domain.isActive()) // Mapeando status
                .build();
    }

    public User toDomain(UserEntity entity) {
        // Usamos o builder do domínio que agora implementa UserDetails
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .role(entity.getRole())
                .phoneNumber(entity.getPhoneNumber())
                .profilePictureUrl(entity.getProfilePictureUrl())
                .createdAt(entity.getCreatedAt())
                .active(entity.isActive())
                .build();
    }
}