package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record BlockProfessionalTimeRequest(
    @NotBlank String professionalId,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotBlank String reason // Ex: "Almoço", "Consulta Médica"
) {}