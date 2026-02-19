package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    StockMovement toDomain(StockMovementEntity entity);

    StockMovementEntity toEntity(StockMovement domain);
}