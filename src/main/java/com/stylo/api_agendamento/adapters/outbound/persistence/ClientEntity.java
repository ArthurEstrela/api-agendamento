package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clients")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientEntity {
    @Id
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String profilePictureUrl;
    private String cpf;
    private LocalDate dateOfBirth;
    private String gender;

    @ElementCollection
    @CollectionTable(name = "client_favorite_professionals", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "professional_id")
    private List<String> favoriteProfessionals; // IDs para agendamento rápido

    private LocalDateTime createdAt;
}