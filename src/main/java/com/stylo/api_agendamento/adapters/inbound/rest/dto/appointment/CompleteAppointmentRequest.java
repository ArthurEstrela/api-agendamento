package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CompleteAppointmentRequest(
    @NotNull String appointmentId,
    @NotNull String paymentMethod,
    BigDecimal serviceFinalPrice, // <--- O nome correto é esse
    List<ProductSaleRequest> soldProducts // <--- Faltava passar isso
) {
    public record ProductSaleRequest(
        String productId,
        Integer quantity
    ) {}
}