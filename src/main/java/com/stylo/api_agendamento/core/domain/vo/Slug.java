package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;

public record Slug(String value) {
    public Slug {
        if (value == null || !value.matches("^[a-z0-9-]+$")) {
            throw new BusinessException("Slug inválido. Use apenas letras minúsculas, números e hífens.");
        }
    }
}