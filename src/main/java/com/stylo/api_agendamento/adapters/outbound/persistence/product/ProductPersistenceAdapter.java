package com.stylo.api_agendamento.adapters.outbound.persistence.product;

import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements IProductRepository {

    private final JpaProductRepository jpaProductRepository;
    private final ProductMapper mapper;

    @Override
    public Product save(Product product) {
        ProductEntity entity = mapper.toEntity(product);
        ProductEntity saved = jpaProductRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(String id) {
        return jpaProductRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Product> findByServiceProviderIdAndIsActiveTrue(String serviceProviderId) {
        return jpaProductRepository.findByServiceProviderIdAndIsActiveTrue(serviceProviderId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}