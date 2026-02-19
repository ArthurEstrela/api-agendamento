package com.stylo.api_agendamento.adapters.outbound.persistence.product;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Product> findById(UUID id) {
        return jpaProductRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        jpaProductRepository.deleteById(id);
    }

    @Override
    public List<Product> findAllActiveByProviderId(UUID providerId) {
        return jpaProductRepository.findByServiceProviderIdAndIsActiveTrue(providerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public PagedResult<Product> findAllByProviderId(UUID providerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<ProductEntity> entityPage = jpaProductRepository.findAllByServiceProviderId(providerId, pageable);

        List<Product> items = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PagedResult<>(
                items,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    @Override
    public List<Product> findLowStock(UUID providerId) {
        return jpaProductRepository.findLowStockProducts(providerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Product> findAllByIds(List<UUID> ids) {
        return jpaProductRepository.findAllByIdIn(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}