package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;

public record Document(String value, DocumentType type) {
    
    public Document {
        if (value == null || value.isBlank()) {
            throw new BusinessException("O documento é obrigatório.");
        }
        if (type == null) {
            throw new BusinessException("O tipo de documento é obrigatório.");
        }

        // Sanitização
        String digits = value.replaceAll("\\D", "");

        // Validação de tamanho baseada no tipo
        if (type == DocumentType.CPF && digits.length() != 11) {
            throw new BusinessException("CPF inválido. Deve conter 11 dígitos.");
        }
        if (type == DocumentType.CNPJ && digits.length() != 14) {
            throw new BusinessException("CNPJ inválido. Deve conter 14 dígitos.");
        }

        value = digits;
    }

    public String getFormatted() {
        if (type == DocumentType.CPF) {
            return value.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        } else {
            return value.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
    }

    public enum DocumentType {
        CPF, CNPJ
    }
}