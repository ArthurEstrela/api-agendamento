package com.stylo.api_agendamento.core.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCreatedEvent(
    UUID appointmentId,
    UUID professionalId,
    String clientName,
    LocalDateTime startTime
) {}