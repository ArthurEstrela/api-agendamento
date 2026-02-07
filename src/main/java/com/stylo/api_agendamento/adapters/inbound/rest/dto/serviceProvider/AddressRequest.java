package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
    @NotBlank String street,
    @NotBlank String number,
    String complement,
    @NotBlank String neighborhood,
    @NotBlank String city,
    @NotBlank String state,
    @NotBlank String zipCode
) {}