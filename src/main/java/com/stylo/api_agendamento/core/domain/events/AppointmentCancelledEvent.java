package com.stylo.api_agendamento.core.domain.events;

import java.time.LocalDateTime;

public record AppointmentCancelledEvent(
    String appointmentId,
    String professionalId,
    LocalDateTime startTime,
    LocalDateTime endTime
) {}