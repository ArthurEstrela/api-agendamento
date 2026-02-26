package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterServiceProviderRequest(
        @NotBlank String ownerName,
        @NotBlank @Email String ownerEmail,
        @NotBlank @Size(min = 6) String ownerPassword,
        @NotBlank String businessName,
        @NotBlank String document,
        @NotBlank String phone,
        @NotNull @Valid AddressRequest address,
        String firebaseUid) {
}
