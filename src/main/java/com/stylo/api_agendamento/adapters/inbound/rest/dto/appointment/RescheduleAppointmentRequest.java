package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record RescheduleAppointmentRequest(
    @NotNull LocalDateTime newStartTime
) {}