package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.stylo.api_agendamento.adapters.outbound.persistence.DocumentVo;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phoneNumber;

    @Embedded // Padronização: CPF do cliente agora é um DocumentVo
    private DocumentVo document;

    private LocalDate dateOfBirth;
    private String gender;

    @ElementCollection
    @CollectionTable(name = "client_favorite_professionals", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "professional_id")
    private List<UUID> favoriteProfessionals;
}