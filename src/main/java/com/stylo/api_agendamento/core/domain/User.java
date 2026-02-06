package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {
    private final String id;
    private final String email;
    private String name;
    private final UserRole role; // CLIENT, SERVICE_PROVIDER, PROFESSIONAL
    private final LocalDateTime createdAt;
    private String phoneNumber;
    private String profilePictureUrl;

    public static User create(String name, String email, UserRole role) {
        if (email == null || !email.contains("@")) {
            throw new BusinessException("E-mail inválido para criação de usuário.");
        }
        return User.builder()
                .name(name)
                .email(email)
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public boolean isProfessional() {
        return UserRole.PROFESSIONAL.equals(this.role);
    }

    public boolean isProvider() {
        return UserRole.SERVICE_PROVIDER.equals(this.role);
    }

    public void updateProfile(String name, String phoneNumber, String url) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = url;
    }
}