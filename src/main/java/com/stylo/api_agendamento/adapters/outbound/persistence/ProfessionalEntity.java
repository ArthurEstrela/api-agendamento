package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "professionals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProfessionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID serviceProviderId;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "professional_services",
        joinColumns = @JoinColumn(name = "professional_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<ServiceEntity> services;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "professional_id")
    private List<DailyAvailabilityEntity> availability;

    private Integer slotInterval;
    private boolean isOwner;
}