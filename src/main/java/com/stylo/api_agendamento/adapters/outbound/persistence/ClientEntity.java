package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNumber; // Valor extraído do VO ClientPhone

    @Column(unique = true)
    private String cpf;

    private LocalDate dateOfBirth;
    private String gender;

    @ElementCollection
    @CollectionTable(name = "client_favorite_professionals", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "professional_id")
    private List<UUID> favoriteProfessionals;
}