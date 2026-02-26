package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import com.stylo.api_agendamento.core.domain.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceMapper {

    // De ServiceEntity para Service (Domínio)
    // O campo na Entity chama-se 'isActive', o getter gerado é 'isActive()'.
    // O MapStruct entende isso como a propriedade 'active'.
    @Mapping(target = "isActive", source = "active")
    Service toDomain(ServiceEntity entity);

    // De Service (Domínio) para ServiceEntity
    // No seu Domínio, o campo é 'isActive', o getter é 'isActive()'.
    // O MapStruct enxerga a propriedade como 'active'.
    @Mapping(target = "isActive", source = "active")
    ServiceEntity toEntity(Service domain);
}