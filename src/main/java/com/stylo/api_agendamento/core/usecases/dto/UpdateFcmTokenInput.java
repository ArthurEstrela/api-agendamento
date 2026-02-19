package com.stylo.api_agendamento.core.usecases.dto;

import java.util.UUID;

public record UpdateFcmTokenInput(
    UUID userId, // ✨ Atualizado para UUID
    String token
) {}