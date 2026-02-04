package com.stylo.api_agendamento.core.domain;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Client extends User {
    private String cpf;
    private List<String> favoriteProfessionals;
    private String gender;
}
