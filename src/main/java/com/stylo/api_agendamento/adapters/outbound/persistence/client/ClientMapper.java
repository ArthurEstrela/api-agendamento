package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import com.stylo.api_agendamento.adapters.outbound.persistence.DocumentVo;
import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

    @Mapping(target = "phoneNumber", source = "phoneNumber.value")
    @Mapping(target = "document", source = "cpf")
    ClientEntity toEntity(Client domain);

    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "cpf", source = "document.value")
    Client toDomain(ClientEntity entity);

    default DocumentVo stringToDocumentVo(String cpf) {
        if (cpf == null || cpf.isBlank()) return null;
        return DocumentVo.builder()
                .value(cpf)
                .type("CPF")
                .build();
    }

    default ClientPhone stringToClientPhone(String phone) {
        return phone != null && !phone.isBlank() ? new ClientPhone(phone) : null;
    }
}