package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.stylo.api_agendamento.adapters.outbound.persistence.DailyAvailabilityEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceEntity;
import com.stylo.api_agendamento.core.domain.RemunerationType;

@Entity
@Table(name = "professionals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfessionalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID serviceProviderId;

    @Column(name = "service_provider_name")
    private String serviceProviderName;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "professional_services", joinColumns = @JoinColumn(name = "professional_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private List<ServiceEntity> services;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "professional_id")
    private List<DailyAvailabilityEntity> availability;

    private Integer slotInterval;
    private boolean isOwner;

    // ProfessionalEntity.java
    @Enumerated(EnumType.STRING)
    private RemunerationType remunerationType;

    @Column(precision = 5, scale = 2)
    private BigDecimal commissionRate; // ex: 0.40 para 40%

    @Column(precision = 10, scale = 2)
    private BigDecimal fixedValue; // ex: 20.00
}