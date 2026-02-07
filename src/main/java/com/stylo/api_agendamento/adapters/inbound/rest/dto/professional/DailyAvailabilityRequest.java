package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record DailyAvailabilityRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull Boolean isWorkingDay,
    LocalTime startTime,
    LocalTime endTime
) {}