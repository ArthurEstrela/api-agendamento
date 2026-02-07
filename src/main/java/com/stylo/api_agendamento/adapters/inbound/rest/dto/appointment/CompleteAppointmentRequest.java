package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CompleteAppointmentRequest(
    @NotNull BigDecimal finalPrice,
    @NotBlank String paymentMethod // PIX, CARD, CASH
) {}