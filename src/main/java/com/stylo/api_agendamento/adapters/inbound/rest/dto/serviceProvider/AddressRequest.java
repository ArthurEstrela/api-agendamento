package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

import com.stylo.api_agendamento.core.domain.vo.Address;
import jakarta.validation.constraints.NotBlank;

// Remova o complement do record e passe nulo no toDomain()
public record AddressRequest(
        @NotBlank String street,
        @NotBlank String number,
        @NotBlank String neighborhood,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String zipCode,
        Double lat,
        Double lng 
) {
    public Address toDomain() {
        return new Address(
                this.street,
                this.number,
                this.neighborhood,
                this.city,
                this.state,
                this.zipCode,
                this.lat, 
                this.lng  
        );
    }
}