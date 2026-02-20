package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
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

    @Embedded
    private DocumentVo document;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    private String gender;

    // ✨ OTIMIZAÇÃO: Usando Set com ElementCollection
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "client_favorite_providers", 
        joinColumns = @JoinColumn(name = "client_id")
    )
    @Column(name = "provider_id")
    @Builder.Default
    private Set<UUID> favoriteProviders = new HashSet<>();
}