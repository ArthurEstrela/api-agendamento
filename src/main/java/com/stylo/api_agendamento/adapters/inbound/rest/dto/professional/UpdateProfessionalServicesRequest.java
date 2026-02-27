package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdateProfessionalServicesRequest(
    @NotNull(message = "A lista de serviços não pode ser nula")
    List<UUID> serviceIds
) {}