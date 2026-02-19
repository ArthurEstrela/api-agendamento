package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceProviderMapper {

    // O MapStruct mapeia automaticamente businessAddress e document 
    // pois possuem o mesmo nome e estrutura equivalente na Entidade e no Domínio.
    ServiceProvider toDomain(ServiceProviderEntity entity);

    ServiceProviderEntity toEntity(ServiceProvider domain);

    // ==== Mapeamentos Nativos para Value Objects ====
    
    default String mapSlugToString(Slug slug) { 
        return slug != null && slug.value() != null ? slug.value() : null; 
    }
    
    default Slug mapStringToSlug(String slug) { 
        return slug != null && !slug.isBlank() ? new Slug(slug) : null; 
    }
}