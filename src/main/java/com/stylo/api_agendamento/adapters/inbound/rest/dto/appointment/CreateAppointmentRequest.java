package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record CreateAppointmentRequest(
        @NotNull(message = "O ID do cliente é obrigatório")
        String clientId,

        @NotNull(message = "O ID do profissional é obrigatório")
        String professionalId,

        @NotNull(message = "A data de início é obrigatória")
        @Future(message = "A data deve ser futura")
        LocalDateTime startTime,

        LocalDateTime endTime, // Front-end envia, podemos receber (mesmo que o backend calcule a duração real dps)

        @NotNull(message = "A lista de itens não pode ser nula")
        List<AppointmentItemRequest> items, // Alinhado com o Front

        String couponCode,
        
        String notes // Alinhado com o Front
) {
    // Record aninhado para espelhar a estrutura enviada pelo React
    public record AppointmentItemRequest(
            @NotNull String referenceId,
            @NotNull String type,
            int quantity
    ) {}
}