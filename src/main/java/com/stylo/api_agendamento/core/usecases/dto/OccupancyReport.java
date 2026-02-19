package com.stylo.api_agendamento.core.usecases.dto;

import java.util.List;
import java.util.UUID;

public record OccupancyReport(
    List<ProfessionalOccupancy> rankings,
    double generalAverageOccupancy
) {
    public record ProfessionalOccupancy(
        UUID professionalId, // ✨ Identificador interno: UUID
        String name,
        int totalAvailableMinutes,
        int occupiedMinutes,
        double occupancyPercentage
    ) {}
}