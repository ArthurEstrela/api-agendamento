package com.stylo.api_agendamento.core.usecases.dto;

import java.time.LocalDateTime;

public record ExternalEvent(
        String externalId, // ✨ String: ID do Google/Outlook (Eles não usam UUID)
        String summary,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}