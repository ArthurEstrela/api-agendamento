package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

@RequiredArgsConstructor
public class CreateProductUseCase {

    private final IProductRepository productRepository;

    public Product execute(CreateProductInput input) {
        Product product = Product.builder()
                .serviceProviderId(input.serviceProviderId())
                .name(input.name())
                .description(input.description())
                .price(input.price())
                .stockQuantity(input.initialStock())
                .isActive(true)
                .build();

        return productRepository.save(product);
    }

    public record CreateProductInput(
            String serviceProviderId,
            String name,
            String description,
            BigDecimal price,
            Integer initialStock
    ) {}
}