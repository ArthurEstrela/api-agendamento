package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "tb_professionals")
@PrimaryKeyJoinColumn(name = "user_id") // O ID aqui é o mesmo da tb_users
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ProfessionalEntity extends UserEntity {

    @Column(columnDefinition = "TEXT")
    private String bio;

    private boolean isOwner;

    @Column(name = "slot_interval")
    private Integer slotInterval;

    // Relacionamento com a empresa/provedor
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_provider_id")
    private ServiceProviderEntity serviceProvider;

    @ManyToMany
    @JoinTable(
        name = "tb_professional_services",
        joinColumns = @JoinColumn(name = "professional_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<ServiceEntity> services;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "professional_id")
    private List<DailyAvailabilityEntity> availability;
}