package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.Getter;

@Getter
public class ClientPhone {
    private final String value;

    public ClientPhone(String value) {
        if (value == null || !value.matches("^\\d{10,11}$")) {
            throw new BusinessException("Telefone inválido. Deve conter apenas números com DDD.");
        }
        this.value = value;
    }
}