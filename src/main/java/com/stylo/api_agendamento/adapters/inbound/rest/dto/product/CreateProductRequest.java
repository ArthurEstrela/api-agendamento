package com.stylo.api_agendamento.adapters.inbound.rest.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "O nome do produto é obrigatório")
        String name,

        String description,

        @NotNull(message = "O preço é obrigatório")
        @Positive(message = "O preço deve ser maior que zero")
        BigDecimal price,

        @NotNull(message = "A quantidade inicial é obrigatória")
        @Min(value = 0, message = "O estoque não pode ser negativo")
        Integer stockQuantity
) {}