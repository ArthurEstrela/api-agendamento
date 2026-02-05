package com.stylo.api_agendamento.core.domain;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Client {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String cpf;
    private LocalDate dateOfBirth;
    private String gender;
    private List<String> favoriteProfessionals; // IDs para facilitar re-agendamento
}