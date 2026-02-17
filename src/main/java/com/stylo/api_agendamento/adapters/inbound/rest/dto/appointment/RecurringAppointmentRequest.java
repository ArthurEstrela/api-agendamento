package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import com.stylo.api_agendamento.core.domain.vo.RecurrenceType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public record RecurringAppointmentRequest(
        @NotNull(message = "ID do profissional é obrigatório")
        String professionalId,

        @NotNull(message = "Lista de serviços é obrigatória")
        List<String> serviceIds,

        @NotNull(message = "Data de início é obrigatória")
        @Future(message = "A data deve ser futura")
        LocalDateTime firstStartTime,

        @NotNull(message = "Tipo de recorrência é obrigatório (DAILY, WEEKLY, BIWEEKLY, MONTHLY)")
        RecurrenceType recurrenceType,

        @Positive(message = "Número de ocorrências deve ser positivo")
        Integer occurrences,

        @Future(message = "Data final deve ser futura")
        LocalDateTime endDate,

        Integer reminderMinutes
) {}