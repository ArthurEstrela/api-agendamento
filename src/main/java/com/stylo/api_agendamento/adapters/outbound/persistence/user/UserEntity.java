package com.stylo.api_agendamento.adapters.outbound.persistence.user;

import com.stylo.api_agendamento.core.domain.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // Alterado para String para manter consistência com o ID do Domínio

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String password; // Adicionado para suportar Autenticação JWT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role; // CLIENT, SERVICE_PROVIDER, PROFESSIONAL

    @Column(nullable = false)
    private boolean active; // Adicionado para controle de acesso (bloqueio/desativação)

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt; // Gerado automaticamente pelo Hibernate

    private String phoneNumber;
    
    private String profilePictureUrl;

    /**
     * PrePersist garante que novos usuários sempre nasçam ativos 
     * se o valor não for especificado.
     */
    @PrePersist
    protected void onCreate() {
        if (!this.active) {
            this.active = true;
        }
    }
}