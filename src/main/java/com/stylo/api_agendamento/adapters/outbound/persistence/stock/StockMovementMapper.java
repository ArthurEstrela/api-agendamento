package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class StockMovementMapper {

    // Se no seu Domínio o campo for 'providerId', mapeamos para 'serviceProviderId'
    // da Entidade
    @Mapping(target = "serviceProviderId", source = "providerId")
    public abstract StockMovementEntity toEntity(StockMovement domain);

    // E vice-versa para voltar ao domínio
    @Mapping(target = "providerId", source = "serviceProviderId")
    public abstract StockMovement toDomain(StockMovementEntity entity);
}