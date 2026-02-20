package com.stylo.api_agendamento.core.usecases.dto;

import java.math.BigDecimal;

public record ProviderSearchCriteria(
    String searchTerm, // Busca por nome do estabelecimento ou nome do serviço
    String city,       // Localização
    Double minRating,  // Avaliação mínima (estrelas)
    BigDecimal minPrice, // Preço mínimo
    BigDecimal maxPrice  // Preço máximo
) {}