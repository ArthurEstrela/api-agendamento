package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateManualAppointmentRequest(
    @NotBlank String professionalId,
    @NotBlank String clientName,
    @NotBlank String clientPhone,
    @NotEmpty List<String> serviceIds,
    @NotNull LocalDateTime startTime,
    String notes
) {}