package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import java.time.LocalDateTime;
import java.util.List;

public record RecurringAppointmentResponse(
        String summaryMessage,
        int totalCreated,
        int totalFailed,
        List<String> createdAppointmentIds,
        List<FailedSlot> failures
) {
    public record FailedSlot(LocalDateTime date, String reason) {}
}