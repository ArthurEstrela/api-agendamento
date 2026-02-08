package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User implements UserDetails {
    private final String id;
    private final String email;
    private String name;
    private String password; // Essencial para UserDetails
    private final UserRole role; // CLIENT, SERVICE_PROVIDER, PROFESSIONAL
    private final LocalDateTime createdAt;
    private String phoneNumber;
    private String profilePictureUrl;
    private boolean active; // Para controle de banimento ou desativação

    public static User create(String name, String email, UserRole role) {
        validateEmail(email);
        return User.builder()
                .name(name)
                .email(email)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // Método para ser usado no fluxo de autenticação/banco
    public static User withPassword(User user, String encodedPassword) {
        user.password = encodedPassword;
        return user;
    }

    private static void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BusinessException("E-mail inválido para criação de usuário.");
        }
    }

    public boolean isProfessional() {
        return UserRole.PROFESSIONAL.equals(this.role);
    }

    public boolean isProvider() {
        return UserRole.SERVICE_PROVIDER.equals(this.role);
    }

    public void updateProfile(String name, String phoneNumber, String url) {
        if (name != null && !name.isBlank()) this.name = name;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = url;
    }

    // --- Implementação Spring Security (UserDetails) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // O padrão "ROLE_" é obrigatório para usar .hasRole() no Spring Security
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.active; // Se não estiver ativo, a conta está "travada"
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.active;
    }
}