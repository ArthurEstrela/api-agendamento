package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import com.stylo.api_agendamento.core.ports.IStockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StockMovementPersistenceAdapter implements IStockMovementRepository {

    private final JpaStockMovementRepository repository;
    private final StockMovementMapper mapper;

    @Override
    public StockMovement save(StockMovement movement) {
        var entity = mapper.toEntity(movement);
        var savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public PagedResult<StockMovement> findAllByProductId(UUID productId, int page, int size) {
        // Ordena pela movimentação mais recente (Auditoria de cima para baixo)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StockMovementEntity> entityPage = repository.findAllByProductId(productId, pageable);

        return toPagedResult(entityPage);
    }

    @Override
    public PagedResult<StockMovement> findAllByProviderId(UUID providerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<StockMovementEntity> entityPage = repository.findAllByServiceProviderId(providerId, pageable);

        return toPagedResult(entityPage);
    }

    // Método auxiliar para evitar duplicação de lógica de mapeamento
    private PagedResult<StockMovement> toPagedResult(Page<StockMovementEntity> entityPage) {
        List<StockMovement> domainItems = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PagedResult<>(
                domainItems,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }
}