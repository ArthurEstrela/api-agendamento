package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Service {
    private final String id;
    private final String serviceProviderId; 
    private String name;
    private String description;
    private Integer duration; 
    private BigDecimal price;

    // Método robusto para criação com todos os campos necessários
    public static Service create(String name, String description, Integer duration, BigDecimal price, String providerId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("O nome do serviço é obrigatório.");
        }
        if (duration == null || duration <= 0) {
            throw new BusinessException("A duração do serviço deve ser maior que zero.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O preço do serviço não pode ser negativo.");
        }
        
        return Service.builder()
                .name(name)
                .description(description)
                .duration(duration)
                .price(price)
                .serviceProviderId(providerId)
                .build();
    }

    public void updateDetails(String name, String description, Integer duration, BigDecimal price) {
        if (duration <= 0) throw new BusinessException("Duração inválida.");
        this.name = name;
        this.description = description;
        this.duration = duration;
        this.price = price;
    }
}