package com.stylo.api_agendamento.core.usecases.dto;

import java.time.LocalDateTime;

public record ExternalEvent(
        String id,
        String summary,
        LocalDateTime startTime,
        LocalDateTime endTime) {
}