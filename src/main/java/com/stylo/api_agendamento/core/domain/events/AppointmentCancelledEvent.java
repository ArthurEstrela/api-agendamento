package com.stylo.api_agendamento.core.domain.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentCancelledEvent(
    UUID appointmentId,
    UUID professionalId,
    UUID clientId,        // ✨ Adicionado para facilitar notificações
    String clientName,    // Snapshot do nome para templates de email
    LocalDateTime startTime,
    LocalDateTime endTime,
    String reason         // ✨ Motivo do cancelamento
) {}