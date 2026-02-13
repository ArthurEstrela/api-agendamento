package com.stylo.api_agendamento.core.usecases.dto;

import java.util.List;

public record OccupancyReport(
    List<ProfessionalOccupancy> rankings,
    double generalAverageOccupancy
) {
    public record ProfessionalOccupancy(
        String professionalId,
        String name,
        int totalAvailableMinutes,
        int occupiedMinutes,
        double occupancyPercentage
    ) {}
}