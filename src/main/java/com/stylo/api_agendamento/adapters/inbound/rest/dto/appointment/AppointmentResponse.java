package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.ServiceProviderRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(
        String id,
        String clientName,
        String professionalName,
        String professionalAvatarUrl, // Foto do profissional
        List<String> serviceNames,
        LocalDateTime startTime,
        LocalDateTime endTime, // Usado para calcular a duração
        BigDecimal totalPrice,
        String status,
        ServiceProviderRequest provider // <-- Aqui entra o seu DTO com os dados do salão
) {
}