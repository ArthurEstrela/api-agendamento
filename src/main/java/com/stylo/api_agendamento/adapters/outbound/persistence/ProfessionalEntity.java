package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.*; // Importa todas as anotações do JPA, incluindo JoinColumn
import lombok.*;
import java.util.List;

@Entity
@Table(name = "professionals")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfessionalEntity {
    @Id
    private String id;

    private String name;
    private String email;
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio; // Alinhado com a interface ProfessionalProfile

    private String serviceProviderId;
    private boolean isOwner;

    @ManyToMany
    @JoinTable(name = "professional_services", joinColumns = @JoinColumn(name = "professional_id"), // Agora reconhecido
                                                                                                    // pelo import
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    private List<ServiceEntity> services; // Serviços que o profissional realiza

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "professional_id")
    private List<DailyAvailabilityEntity> availability;

    private Integer slotInterval; // Intervalo de tempo entre serviços
}