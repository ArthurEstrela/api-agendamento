package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterServiceProviderRequest(
    @NotBlank String ownerName,
    @NotBlank @Email String ownerEmail,
    @NotBlank @Size(min = 6) String ownerPassword,
    @NotBlank String businessName,
    @NotBlank String document, // CPF ou CNPJ
    @NotBlank String phone,
    @NotNull AddressRequest address
) {}
