package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import com.stylo.api_agendamento.adapters.outbound.persistence.ServiceProviderEntity;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServiceProviderMapper {
    @Mapping(source = "businessAddress", target = "businessAddress")
    ServiceProviderEntity toEntity(ServiceProvider domain);

    @Mapping(source = "businessAddress", target = "businessAddress")
    ServiceProvider toDomain(ServiceProviderEntity entity);
}