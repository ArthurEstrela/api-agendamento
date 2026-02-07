package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(
    String id,
    String clientName,
    String professionalName,
    List<String> serviceNames,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BigDecimal totalPrice,
    String status
) {}