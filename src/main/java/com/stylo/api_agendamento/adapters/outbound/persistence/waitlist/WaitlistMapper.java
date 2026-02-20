package com.stylo.api_agendamento.adapters.outbound.persistence.waitlist;

import com.stylo.api_agendamento.core.domain.Waitlist;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WaitlistMapper {

    // ==== Entidade -> Domínio ====
    @Mapping(target = "clientPhone", source = "clientPhone")
    // Se a entidade usar "providerId" e o domínio "serviceProviderId", descomente a linha abaixo:
    // @Mapping(target = "serviceProviderId", source = "providerId") 
    Waitlist toDomain(WaitlistEntity entity);


    // ==== Domínio -> Entidade ====
    // Desempacota o Value Object pegando apenas o valor em String (.value)
    @Mapping(target = "clientPhone", source = "clientPhone.value")
    // Se a sua WaitlistEntity NÃO tiver esse campo no banco, diga ao MapStruct para ignorá-lo:
    @Mapping(target = "serviceProvider.id", ignore = true)
    // Se a entidade tiver o campo com nome diferente (ex: providerId), use isso em vez do ignore:
    // @Mapping(target = "providerId", source = "serviceProviderId")
    WaitlistEntity toEntity(Waitlist domain);


    // ==== Método de Apoio (A Mágica do Value Object) ====
    // O MapStruct chamará este método automaticamente quando precisar converter de String para ClientPhone
    default ClientPhone mapToClientPhone(String phone) {
        return phone != null && !phone.isBlank() ? new ClientPhone(phone) : null;
    }
}