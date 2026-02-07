package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import java.time.LocalDateTime;
import java.util.List;

public record ProfessionalScheduleResponse(
    String professionalId,
    String professionalName,
    List<TimeSlotResponse> busySlots
) {}

record TimeSlotResponse(
    LocalDateTime start,
    LocalDateTime end,
    String type
) {}