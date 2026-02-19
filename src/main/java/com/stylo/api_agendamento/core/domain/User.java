package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class User implements UserDetails {

    private UUID id;
    
    // --- IDENTIFICAÇÃO ---
    private String name;
    private String email;
    private String password; // Hash criptografado
    
    private UserRole role;

    // --- VÍNCULOS (MULTI-TENANT) ---
    private UUID providerId; // Obrigatório para Admin e Profissionais
    private UUID clientId;   // Opcional: Link para a ficha do cliente final

    // --- DADOS DE PERFIL ---
    private String phoneNumber;
    private String profilePictureUrl;
    
    // --- INTEGRAÇÕES ---
    private String fcmToken; // Push Notifications

    // --- SEGURANÇA E ESTADO ---
    private boolean active;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordExpiresAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    /**
     * Factory Method de Criação.
     * Note que a senha não é passada aqui. Ela deve ser injetada via changePassword(),
     * permitindo fluxos de cadastro via OAuth (Google) onde a senha não existe inicialmente.
     */
    public static User create(String name, String email, String phoneNumber, UserRole role) {
        validateEmail(email);
        
        if (name == null || name.isBlank()) {
            throw new BusinessException("Nome do usuário é obrigatório.");
        }
        if (role == null) {
            throw new BusinessException("O papel (Role) do usuário é obrigatório.");
        }

        return User.builder()
                .id(UUID.randomUUID()) // Identidade gerada e blindada
                .name(name)
                .email(email)
                .phoneNumber(phoneNumber)
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

    public void updateProfile(String name, String phoneNumber) {
        if (name != null && !name.isBlank()) this.name = name;
        if (phoneNumber != null) this.phoneNumber = phoneNumber;
        this.updatedAt = LocalDateTime.now();
    }

    // ✨ Método acionado pelo UpdateProfilePictureUseCase
    public void updateAvatar(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
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

    // ✨ Métodos exatos exigidos pelo RequestPasswordResetUseCase
    public void generatePasswordResetToken() {
        this.resetPasswordToken = UUID.randomUUID().toString();
        this.resetPasswordExpiresAt = LocalDateTime.now().plusHours(1); // Hard limit de 1 hora
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isResetTokenValid() {
        return this.resetPasswordToken != null 
                && this.resetPasswordExpiresAt != null 
                && LocalDateTime.now().isBefore(this.resetPasswordExpiresAt);
    }

    public void clearPasswordResetToken() {
        this.resetPasswordToken = null;
        this.resetPasswordExpiresAt = null;
        this.updatedAt = LocalDateTime.now();
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

    // --- IMPLEMENTAÇÃO USER DETAILS (Integração Limpa com Spring Security) ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return Collections.emptyList();
        return role.getAuthorities();
    }

    @Override
    public String getUsername() {
        return this.email; // O Spring Security usará o E-mail como Login
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
            throw new BusinessException("E-mail com formato inválido.");
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