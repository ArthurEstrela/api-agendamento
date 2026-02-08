package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

import com.stylo.api_agendamento.core.domain.vo.Address;
import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank String street,
        @NotBlank String number,
        String complement,
        @NotBlank String neighborhood,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode) {
    /**
     * Converte o DTO de entrada para o Value Object de Domínio.
     */
    public Address toDomain() {
        return new Address(
                this.street,
                this.number,
                this.complement,
                this.neighborhood,
                this.city,
                this.state,
                this.zipCode);
    }
}