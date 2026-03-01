package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateProfessionalRequest(
        @NotNull(message = "O ID do estabelecimento é obrigatório") UUID providerId,

        @NotBlank(message = "O nome é obrigatório") String name,

        String email,
        String bio,
        BigDecimal commissionPercentage,
        List<UUID> serviceIds,
        Boolean isOwner) {
}