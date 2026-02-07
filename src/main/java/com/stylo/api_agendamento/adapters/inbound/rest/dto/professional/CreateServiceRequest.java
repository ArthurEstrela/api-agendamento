package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceRequest(
    @NotBlank String name,
    String description,
    @NotNull BigDecimal price,
    @NotNull Integer duration, // em minutos
    @NotBlank String categoryId
) {}