package com.stylo.api_agendamento.adapters.outbound.persistence.user;

import com.stylo.api_agendamento.core.domain.User;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class UserMapper {

    public UserEntity toEntity(User domain) {
        if (domain == null) return null;

        return UserEntity.builder()
                .id(domain.getId() != null ? UUID.fromString(domain.getId()) : null)
                .name(domain.getName())
                .email(domain.getEmail())
                .password(domain.getPassword())
                .role(domain.getRole())
                .phoneNumber(domain.getPhoneNumber())
                .profilePictureUrl(domain.getProfilePictureUrl())
                .createdAt(domain.getCreatedAt())
                .active(domain.isActive())
                .fcmToken(domain.getFcmToken()) // ✨ Adicionado: Salva no banco
                .build();
    }

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;

        return User.builder()
                .id(entity.getId() != null ? entity.getId().toString() : null)
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .role(entity.getRole())
                .phoneNumber(entity.getPhoneNumber())
                .profilePictureUrl(entity.getProfilePictureUrl())
                .createdAt(entity.getCreatedAt())
                .active(entity.isActive())
                .fcmToken(entity.getFcmToken()) // ✨ Adicionado: Lê do banco
                .build();
    }
}