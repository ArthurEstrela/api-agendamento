package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateAvailabilityRequest(
    @NotEmpty List<DailyAvailabilityRequest> availabilities,
    @NotNull Integer slotInterval // em minutos, ex: 30
) {}