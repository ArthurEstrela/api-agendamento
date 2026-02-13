package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record UpdateServiceRequest(
    @NotBlank(message = "O nome do serviço não pode estar em branco")
    String name,
    
    String description,
    
    @NotNull(message = "A duração é obrigatória")
    @Positive(message = "A duração deve ser um valor positivo")
    Integer duration,
    
    @NotNull(message = "O preço é obrigatório")
    @Positive(message = "O preço deve ser um valor positivo")
    BigDecimal price
) {}