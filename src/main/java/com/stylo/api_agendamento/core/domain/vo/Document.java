package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;

public record Document(String value, String type) {
    public Document {
        // Aqui você pode adicionar uma biblioteca de validação de CPF/CNPJ real
        if (value == null || value.isEmpty()) {
            throw new BusinessException("O documento é obrigatório.");
        }
    }
}