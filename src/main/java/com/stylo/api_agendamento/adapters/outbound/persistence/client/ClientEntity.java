package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.DocumentVo;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ClientEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Embedded // Value Object embutido nas colunas desta mesma tabela
    private DocumentVo document;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    private String gender;

    // ✨ OTIMIZAÇÃO: Lazy Loading explícito e inicialização segura
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "client_favorite_professionals", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "professional_id")
    @Builder.Default
    private List<UUID> favoriteProfessionals = new ArrayList<>();
}