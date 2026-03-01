package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper;
import com.stylo.api_agendamento.adapters.outbound.persistence.mapper.AvailabilityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { ServiceMapper.class,
        AvailabilityMapper.class })
public interface ProfessionalMapper {

    // Lê do banco (Entity -> Domain)
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isOwner", source = "owner") // Aqui tá certo, alvo no Domínio é isOwner
    Professional toDomain(ProfessionalEntity entity);

    // Salva no banco (Domain -> Entity)
    @Mapping(target = "isActive", source = "active")
    @Mapping(target = "isOwner", source = "owner") // ✨ INVERTE AQUI! Alvo na Entidade é "owner", e a fonte do Domínio é "isOwner"
    ProfessionalEntity toEntity(Professional domain);
}