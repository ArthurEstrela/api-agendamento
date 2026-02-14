package com.stylo.api_agendamento.core.domain.events;

import java.time.LocalDateTime;

public record AppointmentCreatedEvent(
    String appointmentId,
    String professionalId,
    String clientName,
    LocalDateTime startTime
) {}