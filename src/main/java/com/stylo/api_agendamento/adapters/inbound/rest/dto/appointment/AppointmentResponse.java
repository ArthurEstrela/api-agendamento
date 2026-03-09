package com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.ServiceProviderRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AppointmentResponse(
                String id,
                UUID providerId, // Necessário para o frontend saber de quem é
                UUID professionalId, // Necessário para filtros
                UUID clientId, // Necessário para avaliação
                String clientName,
                String clientPhone, // ✨ ESSENCIAL: Para o botão do WhatsApp funcionar
                String professionalName,
                String professionalAvatarUrl,
                List<String> serviceNames, // ✨ ESSENCIAL: Para mostrar os nomes dos serviços
                LocalDateTime startTime,
                LocalDateTime endTime,
                BigDecimal totalPrice, // ✨ ESSENCIAL: Para mostrar o valor
                String status,
                String notes, // ✨ ESSENCIAL: O frontend usa para detectar se é "Bloqueio"
                ServiceProviderRequest provider) {
}