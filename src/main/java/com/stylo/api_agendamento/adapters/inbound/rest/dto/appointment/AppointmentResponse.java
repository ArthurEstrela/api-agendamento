package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.ServiceProviderRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(
                String id,
                String clientName,
                String professionalName,
                String professionalAvatarUrl,
                List<String> serviceNames, // <-- Retorna nomes em vez de objetos completos
                LocalDateTime startTime,
                LocalDateTime endTime,
                BigDecimal totalPrice, // <-- Usa totalPrice em vez de totalAmount
                String status,
                ServiceProviderRequest provider) {
}