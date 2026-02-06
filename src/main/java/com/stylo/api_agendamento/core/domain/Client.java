package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Collections;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Client {
    private final String id;
    private String name;
    private final String email;
    private ClientPhone phoneNumber; 
    private final String cpf;
    private LocalDate dateOfBirth;
    private String gender;
    private List<String> favoriteProfessionals;

    public static Client create(String name, String email, ClientPhone phone, String cpf) {
        if (email == null || !email.contains("@")) {
            throw new BusinessException("E-mail de cliente inválido.");
        }
        return Client.builder()
                .name(name)
                .email(email)
                .phoneNumber(phone)
                .cpf(cpf)
                .favoriteProfessionals(Collections.emptyList())
                .build();
    }

    public void addFavoriteProfessional(String professionalId) {
        if (!this.favoriteProfessionals.contains(professionalId)) {
            this.favoriteProfessionals.add(professionalId);
        }
    }

    public void updateContact(String name, ClientPhone phone) {
        if (name == null || name.isBlank()) throw new BusinessException("Nome inválido.");
        this.name = name;
        this.phoneNumber = phone;
    }
}