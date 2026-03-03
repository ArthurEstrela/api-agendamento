package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import com.fasterxml.jackson.annotation.JsonFormat; // ✨ NOVO IMPORT
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record DailyAvailabilityRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull Boolean isWorkingDay,

        // ✨ ENSINA O JAVA A LER "09:00" VINDO DO FRONTEND
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime startTime,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime endTime) {
}