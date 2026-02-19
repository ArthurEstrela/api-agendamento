package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Service {

    private UUID id;
    private UUID serviceProviderId;

    private String name;
    private String description;
    
    // Duração em minutos
    private Integer duration; 
    
    private BigDecimal price;

    private boolean isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Service create(UUID serviceProviderId, String name, String description, 
                                 Integer duration, BigDecimal price) {
        
        if (serviceProviderId == null) {
            throw new BusinessException("O serviço deve pertencer a um estabelecimento (Provider).");
        }
        
        validateServiceData(name, duration, price);

        return Service.builder()
                .id(UUID.randomUUID()) // Identidade gerada
                .serviceProviderId(serviceProviderId)
                .name(name)
                .description(description)
                .duration(duration)
                .price(price)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void update(String name, String description, Integer duration, BigDecimal price) {
        // Atualiza apenas se o valor for passado (Partial Update)
        if (name != null && !name.isBlank()) {
            this.name = name;
        }

        if (description != null) { 
            this.description = description;
        }

        if (duration != null) {
            if (duration <= 0) throw new BusinessException("A duração do serviço deve ser maior que zero.");
            this.duration = duration;
        }

        if (price != null) {
            if (price.compareTo(BigDecimal.ZERO) < 0) throw new BusinessException("O preço do serviço não pode ser negativo.");
            this.price = price;
        }

        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    // --- VALIDAÇÕES AUXILIARES ---

    private static void validateServiceData(String name, Integer duration, BigDecimal price) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("O nome do serviço é obrigatório.");
        }
        if (duration == null || duration <= 0) {
            throw new BusinessException("A duração do serviço deve ser maior que zero.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O preço do serviço não pode ser negativo.");
        }
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return Objects.equals(id, service.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}