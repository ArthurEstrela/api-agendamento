package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;

public record CreateAppointmentRequest(
        @NotNull(message = "O ID do cliente é obrigatório")
        String clientId,

        @NotNull(message = "O ID do profissional é obrigatório")
        String professionalId,

        @NotNull(message = "Selecione ao menos um serviço")
        List<String> serviceIds,

        @NotNull
        @Future(message = "A data deve ser futura")
        LocalDateTime startTime,

        @Positive(message = "Os minutos de lembrete devem ser positivos")
        Integer reminderMinutes
) {}