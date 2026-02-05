package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "tb_clients")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class ClientEntity extends UserEntity {

    @Column(unique = true)
    private String cpf;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String gender;

    @ElementCollection
    @CollectionTable(
        name = "tb_client_favorite_professionals", 
        joinColumns = @JoinColumn(name = "client_id")
    )
    @Column(name = "professional_id")
    private List<String> favoriteProfessionals;
}