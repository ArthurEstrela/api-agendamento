package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddItemRequest(
        @NotNull(message = "ID do produto é obrigatório")
        String productId,

        @Min(value = 1, message = "Quantidade deve ser pelo menos 1")
        Integer quantity
) {}