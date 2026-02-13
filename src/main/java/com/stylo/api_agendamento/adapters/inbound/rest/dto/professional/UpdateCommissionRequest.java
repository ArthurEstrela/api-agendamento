package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import com.stylo.api_agendamento.core.domain.RemunerationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateCommissionRequest(
    @NotNull(message = "O tipo de remuneração (FIXED ou PERCENTAGE) é obrigatório")
    RemunerationType type,
    
    @NotNull(message = "O valor da remuneração é obrigatório")
    @PositiveOrZero(message = "O valor da remuneração não pode ser negativo")
    BigDecimal value
) {}