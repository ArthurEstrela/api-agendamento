package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ServiceProviderRequest(
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank String password,
    @NotBlank String businessName,
    @NotBlank String document, // CPF/CNPJ
    @NotNull AddressRequest address,
    @NotBlank String phone
) {}