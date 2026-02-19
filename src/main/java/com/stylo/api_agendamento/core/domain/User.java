package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class User implements UserDetails {

    private UUID id;
    
    // Identificação
    private String name;
    private String email;
    private String password; // Hash criptografado
    
    private UserRole role;

    // --- VÍNCULOS ---
    // Se o usuário for um Profissional ou Admin de Salão, ele tem um providerId
    private UUID providerId; 
    
    // Se o usuário for um Cliente final, ele pode ter um perfil de cliente vinculado (opcional)
    private UUID clientId;

    // --- DADOS DE PERFIL ---
    private String phoneNumber;
    private String profilePictureUrl;
    
    // --- INTEGRAÇÕES ---
    private String fcmToken; // Firebase Cloud Messaging

    // --- SEGURANÇA E ESTADO ---
    private boolean active;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordExpiresAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static User create(String name, String email, String encodedPassword, UserRole role) {
        validateEmail(email);
        
        if (name == null || name.isBlank()) {
            throw new BusinessException("Nome do usuário é obrigatório.");
        }
        if (role == null) {
            throw new BusinessException("O papel (Role) do usuário é obrigatório.");
        }

        return User.builder()
                .id(UUID.randomUUID()) // Identidade gerada
                .name(name)
                .email(email)
                .password(encodedPassword)
                .role(role)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO: PERFIL E VÍNCULOS ---

    public void linkProvider(UUID providerId) {
        if (providerId == null) throw new BusinessException("ID do estabelecimento inválido.");
        this.providerId = providerId;
        this.updatedAt = LocalDateTime.now();
    }

    public void linkClient(UUID clientId) {
        if (clientId == null) throw new BusinessException("ID do cliente inválido.");
        this.clientId = clientId;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String name, String phoneNumber, String profilePictureUrl) {
        if (name != null && !name.isBlank()) this.name = name;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        if (profilePictureUrl != null) this.profilePictureUrl = profilePictureUrl;
        
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFcmToken(String token) {
        this.fcmToken = token;
        this.updatedAt = LocalDateTime.now();
    }

    // --- MÉTODOS DE NEGÓCIO: SEGURANÇA ---

    public void changePassword(String newEncodedPassword) {
        if (newEncodedPassword == null || newEncodedPassword.isBlank()) {
            throw new BusinessException("A nova senha não pode ser vazia.");
        }
        this.password = newEncodedPassword;
        this.updatedAt = LocalDateTime.now();
    }

    public void requestPasswordReset() {
        this.resetPasswordToken = UUID.randomUUID().toString();
        this.resetPasswordExpiresAt = LocalDateTime.now().plusHours(1); // Token válido por 1 hora
        this.updatedAt = LocalDateTime.now();
    }

    public void completePasswordReset(String newEncodedPassword) {
        this.password = newEncodedPassword;
        this.resetPasswordToken = null; // Invalida o token usado
        this.resetPasswordExpiresAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isResetTokenValid(String token) {
        return token != null
                && token.equals(this.resetPasswordToken)
                && this.resetPasswordExpiresAt != null
                && this.resetPasswordExpiresAt.isAfter(LocalDateTime.now());
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    // --- VERIFICAÇÕES DE PAPEL (Helpers) ---

    public boolean isProfessional() {
        return UserRole.PROFESSIONAL.equals(this.role);
    }

    public boolean isProviderAdmin() {
        return UserRole.SERVICE_PROVIDER.equals(this.role);
    }

    public boolean isClient() {
        return UserRole.CLIENT.equals(this.role);
    }

    // --- IMPLEMENTAÇÃO USER DETAILS (Spring Security) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Delega para o Enum UserRole resolver as permissões
        return role.getAuthorities();
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
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

    // --- AUXILIARES ---

    private static void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BusinessException("E-mail inválido.");
        }
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}