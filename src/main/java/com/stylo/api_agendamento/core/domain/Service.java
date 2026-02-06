package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Service {
    private final String id;
    private final String serviceProviderId; // ID do profissional que oferece o serviço
    private String name;
    private String description;
    private Integer duration; // Em minutos
    private BigDecimal price;

    public static Service create(String name, Integer duration, BigDecimal price) {
        if (duration == null || duration <= 0) {
            throw new BusinessException("A duração do serviço deve ser maior que zero.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O preço do serviço não pode ser negativo.");
        }
        return Service.builder()
                .name(name)
                .duration(duration)
                .price(price)
                .build();
    }

    public void updateDetails(String name, Integer duration, BigDecimal price) {
        if (duration <= 0)
            throw new BusinessException("Duração inválida.");
        this.name = name;
        this.duration = duration;
        this.price = price;
    }

    public static Service create(String name, Integer duration, BigDecimal price, String providerId) {
        if (duration == null || duration <= 0) {
            throw new BusinessException("A duração do serviço deve ser maior que zero.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O preço do serviço não pode ser negativo.");
        }
        return Service.builder()
                .name(name)
                .duration(duration)
                .price(price)
                .serviceProviderId(providerId)
                .build();
    }
}