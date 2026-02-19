package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;

@Entity
@Table(name = "services")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder(toBuilder = true)
public class ServiceEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ✨ DESACOPLAMENTO (DDD): Apenas o ID, sem ManyToOne carregando a entidade inteira
    @Column(name = "service_provider_id", nullable = false)
    private UUID serviceProviderId;

    // ✨ SINCRONIA: O campo de categoria que adicionamos no Domínio
    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false)
    private String name;

    // ✨ SINCRONIA: Faltava a descrição que existe no Domínio
    @Column(columnDefinition = "TEXT")
    private String description;

    // ✨ PRECISÃO FINANCEIRA
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer duration; // em minutos

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}