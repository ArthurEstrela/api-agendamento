package com.stylo.api_agendamento.adapters.outbound.persistence;

import com.stylo.api_agendamento.core.domain.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_users")
@Inheritance(strategy = InheritanceType.JOINED) // Estratégia robusta para perfis distintos
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class UserEntity { // 'abstract' evita instanciar um usuário sem perfil

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}