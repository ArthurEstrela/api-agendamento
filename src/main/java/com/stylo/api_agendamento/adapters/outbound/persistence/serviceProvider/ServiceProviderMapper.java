package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import com.stylo.api_agendamento.adapters.outbound.persistence.AddressVo;
import com.stylo.api_agendamento.adapters.outbound.persistence.DocumentVo;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Address;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceProviderMapper {

    @Mapping(target = "services", ignore = true)
    @Mapping(target = "publicProfileSlug", source = "publicProfileSlug")
    @Mapping(target = "businessAddress", source = "businessAddress")
    @Mapping(target = "document", source = "document")
    ServiceProviderEntity toEntity(ServiceProvider domain);

    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "maxNoShowsAllowed", ignore = true)
    @Mapping(target = "publicProfileSlug", source = "publicProfileSlug")
    @Mapping(target = "businessAddress", source = "businessAddress")
    @Mapping(target = "document", source = "document")
    ServiceProvider toDomain(ServiceProviderEntity entity);

    // Conversores Customizados para o Slug
    default String mapSlugToString(Slug slug) {
        return slug != null ? slug.value() : null;
    }

    default Slug mapStringToSlug(String string) {
        return string != null ? new Slug(string) : null;
    }

    // Sub-mapeadores explícitos para Value Objects (Resolve o erro "erroneous element null")
    AddressVo mapAddressToVo(Address address);
    Address mapVoToAddress(AddressVo addressVo);

    DocumentVo mapDocumentToVo(Document document);
    Document mapVoToDocument(DocumentVo documentVo);
}