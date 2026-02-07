package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import com.stylo.api_agendamento.adapters.outbound.persistence.DocumentVo;
import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.domain.vo.Document;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "phoneNumber", source = "phoneNumber.value")
    // Mapeamos o campo 'cpf' do domínio para o objeto 'document' da entidade
    @Mapping(target = "document", source = "cpf", qualifiedByName = "cpfToDocumentVo")
    @Mapping(target = "favoriteProfessionals", source = "favoriteProfessionals", qualifiedByName = "stringListToUuidList")
    ClientEntity toEntity(Client domain);

    @Mapping(target = "phoneNumber.value", source = "phoneNumber")
    // Mapeamos o 'document' da entidade de volta para o campo 'cpf' do domínio
    @Mapping(target = "cpf", source = "document.value")
    @Mapping(target = "favoriteProfessionals", source = "favoriteProfessionals", qualifiedByName = "uuidListToStringList")
    Client toDomain(ClientEntity entity);

    @Named("cpfToDocumentVo")
    default DocumentVo cpfToDocumentVo(String cpf) {
        if (cpf == null) return null;
        return DocumentVo.builder()
                .value(cpf)
                .type("CPF") // Como é a entidade Client, assumimos CPF
                .build();
    }

    // Correção para o Record Document (usando .value() e .type() em vez de getters)
    default DocumentVo mapDocumentToVo(Document document) {
        if (document == null) return null;
        return DocumentVo.builder()
                .value(document.value()) // Corrigido: records usam nome do campo como método
                .type(document.type())   // Corrigido: records usam nome do campo como método
                .build();
    }

    @Named("stringListToUuidList")
    default List<UUID> stringListToUuidList(List<String> list) {
        if (list == null) return null;
        return list.stream().map(UUID::fromString).collect(Collectors.toList());
    }

    @Named("uuidListToStringList")
    default List<String> uuidListToStringList(List<UUID> list) {
        if (list == null) return null;
        return list.stream().map(UUID::toString).collect(Collectors.toList());
    }
}