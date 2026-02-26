package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import com.stylo.api_agendamento.core.domain.Service;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceMapper {
    
    @Mapping(target = "serviceProviderId", source = "serviceProviderId")
    @Mapping(target = "isActive", source = "active") // ✨ Entity (active) para Domain (isActive)
    Service toDomain(ServiceEntity entity);

    @Mapping(target = "serviceProviderId", source = "serviceProviderId")
    @Mapping(target = "active", source = "isActive") // ✨ Domain (isActive) para Entity (active)
    ServiceEntity toEntity(Service domain);
}