package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

import java.util.List;

import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;

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
    List<PaymentMethod> paymentMethods,
    SocialLinksRequest socialLinks,
    AddressRequest businessAddress,
    String logoUrl,
    String bannerUrl
) {}