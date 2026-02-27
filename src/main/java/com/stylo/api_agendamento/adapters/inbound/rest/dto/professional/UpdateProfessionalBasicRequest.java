package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfessionalBasicRequest(
        @NotBlank(message = "O nome não pode ficar em branco") String name,

        String bio) {
}