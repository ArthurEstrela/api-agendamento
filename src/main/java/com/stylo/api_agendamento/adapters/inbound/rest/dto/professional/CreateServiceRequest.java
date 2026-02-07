package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateServiceRequest(
    @NotBlank String name,
    String description,
    @NotNull @Positive BigDecimal price,
    @NotNull @Positive Integer duration, // O campo correto no seu arquivo é 'duration'
    @NotBlank String categoryId
) {}