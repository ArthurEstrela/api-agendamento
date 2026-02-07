package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;

public record DailyAvailabilityRequest(
    @NotBlank String dayOfWeek,
    boolean isOpen,
    LocalTime startTime,
    LocalTime endTime
) {}