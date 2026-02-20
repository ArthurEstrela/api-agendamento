package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceProviderMapper {

    @Mapping(target = "publicProfileSlug", source = "publicProfileSlug.value")
    @Mapping(target = "businessAddress", source = "businessAddress")
    ServiceProviderEntity toEntity(ServiceProvider domain);

    @Mapping(target = "publicProfileSlug", source = "publicProfileSlug")
    @Mapping(target = "businessAddress", source = "businessAddress")
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "maxNoShowsAllowed", ignore = true)
    ServiceProvider toDomain(ServiceProviderEntity entity);

    // ✨ RESOLVE O ERRO DO SLUG: Converte o Value Object Slug para String
    default String mapSlugToString(Slug slug) {
        return slug != null ? slug.value() : null;
    }

    // ✨ RESOLVE O ERRO DO SLUG NA VOLTA: Converte String para o Value Object Slug
    default Slug mapStringToSlug(String slug) {
        return slug != null ? new Slug(slug) : null;
    }

    // ✨ RESOLVE O ERRO DO ADDRESS: 
    // O MapStruct chamará automaticamente os mappers de AddressVo <-> Address 
    // se você deixar os nomes dos campos iguais.
}