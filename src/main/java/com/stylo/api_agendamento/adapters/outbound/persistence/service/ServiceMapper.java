package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import com.stylo.api_agendamento.core.domain.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceMapper {
    
    // Mapeia o ID da entidade (Foreign Key) de volta para o campo puro no domínio
    @Mapping(target = "serviceProviderId", source = "serviceProviderId")
    Service toDomain(ServiceEntity entity);

    // Cria uma referência Lazy usando apenas o ID para salvar no banco de forma otimizada
    @Mapping(target = "serviceProviderId", source = "serviceProviderId")
    ServiceEntity toEntity(Service domain);
}