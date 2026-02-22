package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;

public record Address(
    String street,
    String number,
    String complement,
    String neighborhood,
    String city,
    String state,
    String zipCode,
    Double lat, // ✨ NOVO
    Double lng  // ✨ NOVO
) {
    public Address {
        if (street == null || street.isBlank()) throw new BusinessException("A rua é obrigatória.");
        if (city == null || city.isBlank()) throw new BusinessException("A cidade é obrigatória.");
        if (state == null || state.isBlank()) throw new BusinessException("O estado é obrigatório.");
        
        // Validação básica de CEP (apenas números, 8 dígitos)
        if (zipCode != null && !zipCode.isBlank()) {
            String cleanZip = zipCode.replaceAll("\\D", "");
            if (cleanZip.length() != 8) throw new BusinessException("CEP inválido.");
            zipCode = cleanZip; // Armazena limpo
        }
    }
}