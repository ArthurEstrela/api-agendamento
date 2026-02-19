package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;

public record ClientPhone(String value) {
    
    public ClientPhone {
        if (value == null || value.isBlank()) {
            throw new BusinessException("O telefone é obrigatório.");
        }
        
        // Sanitização: Remove tudo que não é dígito
        String digits = value.replaceAll("\\D", "");

        if (!digits.matches("^\\d{10,11}$")) {
            throw new BusinessException("Telefone inválido. Informe DDD + Número (10 ou 11 dígitos).");
        }
        
        value = digits; // Armazena apenas os números
    }

    public String getFormatted() {
        if (value.length() == 11) { // Celular: (XX) XXXXX-XXXX
            return value.replaceFirst("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        } else { // Fixo: (XX) XXXX-XXXX
            return value.replaceFirst("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        }
    }
}