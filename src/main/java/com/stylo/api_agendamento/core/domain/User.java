package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;

@Getter
@Builder(toBuilder = true) // ✨ Adicionado toBuilder para facilitar atualizações
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User implements UserDetails {
    private final String id;
    private final String email;
    private String name;
    private String password;
    private final UserRole role;
    private final LocalDateTime createdAt;
    private String phoneNumber;
    private String profilePictureUrl;
    private boolean active;
    private String fcmToken;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordExpiresAt;

    // ✨ NOVO CAMPO: Vincula o usuário ao seu ServiceProvider (se ele for um)
    private String providerId;

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

    public static User withPassword(User user, String encodedPassword) {
        // Como os campos são finais ou o builder é usado, idealmente recriamos
        return user.toBuilder().password(encodedPassword).build();
    }

    // ✨ Método para vincular o provider
    public User linkProvider(String providerId) {
        return this.toBuilder().providerId(providerId).build();
    }

    public void updateFcmToken(String token) {
        this.fcmToken = token;
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
        if (name != null && !name.isBlank())
            this.name = name;
        this.phoneNumber = phoneNumber;
        this.profilePictureUrl = url;
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
        return this.active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.active;
    }

    public void generatePasswordResetToken() {
        this.resetPasswordToken = java.util.UUID.randomUUID().toString();
        this.resetPasswordExpiresAt = LocalDateTime.now().plusHours(1);
    }

    public void clearPasswordResetToken() {
        this.resetPasswordToken = null;
        this.resetPasswordExpiresAt = null;
    }

    public boolean isResetTokenValid(String token) {
        return token != null
                && token.equals(this.resetPasswordToken)
                && this.resetPasswordExpiresAt != null
                && this.resetPasswordExpiresAt.isAfter(LocalDateTime.now());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // ✨ A mágica acontece aqui: O usuário herda todas as permissões do seu cargo
        return role.getAuthorities();
    }
}