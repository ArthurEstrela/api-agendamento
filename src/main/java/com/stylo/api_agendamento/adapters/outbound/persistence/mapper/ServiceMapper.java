package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import com.stylo.api_agendamento.adapters.outbound.persistence.ServiceEntity;
import com.stylo.api_agendamento.core.domain.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServiceMapper {
    
    // Mapeia o serviceProviderId do domínio para o objeto aninhado na Entidade
    @Mapping(target = "serviceProvider.id", source = "serviceProviderId")
    ServiceEntity toEntity(Service domain);

    // Mapeia o ID do objeto aninhado na Entidade de volta para o campo do domínio
    @Mapping(target = "serviceProviderId", source = "serviceProvider.id")
    Service toDomain(ServiceEntity entity);
}