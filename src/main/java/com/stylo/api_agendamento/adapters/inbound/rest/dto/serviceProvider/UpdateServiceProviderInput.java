package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

public record UpdateServiceProviderInput(
    String name,
    String businessName,
    String publicProfileSlug,
    String documentType,
    String document,
    String businessPhone,
    Integer cancellationMinHours,
    String pixKey,
    String pixKeyType,
    SocialLinksRequest socialLinks,
    AddressRequest businessAddress,
    String logoUrl,
    String bannerUrl
) {}