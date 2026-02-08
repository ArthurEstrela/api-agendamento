package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

public record UpdateServiceProviderInput(
    String name,
    String phoneNumber,
    String slug,
    AddressRequest address,
    String logoUrl
) {}