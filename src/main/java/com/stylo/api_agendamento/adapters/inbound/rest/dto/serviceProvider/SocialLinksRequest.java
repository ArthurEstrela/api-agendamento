package com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider;

public record SocialLinksRequest(
    String instagram,
    String facebook,
    String website,
    String whatsapp
) {}