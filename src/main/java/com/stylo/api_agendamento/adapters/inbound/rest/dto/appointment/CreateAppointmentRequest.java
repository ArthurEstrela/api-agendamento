package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateAppointmentRequest(
    @NotBlank String clientId,
    @NotBlank String professionalId,
    @NotEmpty List<String> serviceIds,
    @NotNull LocalDateTime startTime,
    Integer reminderMinutes
) {}