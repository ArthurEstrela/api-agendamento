package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stylo.api_agendamento.adapters.outbound.persistence.DailyAvailabilityEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import com.stylo.api_agendamento.core.domain.RemunerationType;

@Entity
@Table(name = "professionals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ProfessionalEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_provider_id", nullable = false)
    private UUID serviceProviderId;

    @Column(name = "service_provider_name")
    private String serviceProviderName;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    // ✨ Sincronizado com o Domínio: Mapeamento da lista de "Tags" (Ex: Barbeiro, Visagista)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "professional_specialties", joinColumns = @JoinColumn(name = "professional_id"))
    @Column(name = "specialty")
    @Builder.Default
    private List<String> specialties = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "professional_services", 
               joinColumns = @JoinColumn(name = "professional_id"), 
               inverseJoinColumns = @JoinColumn(name = "service_id"))
    @Builder.Default
    private List<ServiceEntity> services = new ArrayList<>();

    // ✨ Otimizado: Trocado EAGER para LAZY (Evita derrubar a memória do banco em buscas de lista)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "professional_id")
    @Builder.Default
    private List<DailyAvailabilityEntity> availability = new ArrayList<>();

    @Column(name = "slot_interval")
    private Integer slotInterval;
    
    @Column(name = "is_owner")
    private boolean isOwner;

    @Enumerated(EnumType.STRING)
    @Column(name = "remuneration_type")
    private RemunerationType remunerationType;

    // ✨ Proteção Monetária
    @Column(name = "remuneration_value", precision = 19, scale = 2)
    private BigDecimal remunerationValue;
}