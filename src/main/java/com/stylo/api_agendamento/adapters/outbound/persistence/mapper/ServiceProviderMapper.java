package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import com.stylo.api_agendamento.adapters.outbound.persistence.ServiceProviderEntity;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Address;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServiceProviderMapper {

    @Mapping(target = "businessAddress", source = "businessAddress")
    @Mapping(target = "document", source = "document")
    @Mapping(target = "publicProfileSlug", source = "publicProfileSlug")
    ServiceProviderEntity toEntity(ServiceProvider domain);

    @Mapping(target = "businessAddress", source = "businessAddress")
    @Mapping(target = "document", source = "document")
    @Mapping(target = "publicProfileSlug", source = "publicProfileSlug")
    ServiceProvider toDomain(ServiceProviderEntity entity);

    // Mapeamento corrigido para Record
    default String map(Slug slug) { 
        return slug != null ? slug.value() : null; 
    }
    
    default Slug map(String slug) { 
        return slug != null ? new Slug(slug) : null; 
    }
}