package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import com.stylo.api_agendamento.core.ports.IStockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}