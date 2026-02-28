package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper;
import com.stylo.api_agendamento.adapters.outbound.persistence.mapper.AvailabilityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping; // ✨ IMPORT NECESSÁRIO
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { ServiceMapper.class,
        AvailabilityMapper.class })
public interface ProfessionalMapper {

    // ✨ Força o MapStruct a entender que isActive() e setActive() representam a propriedade "active"
    @Mapping(target = "isActive", source = "active")
    Professional toDomain(ProfessionalEntity entity);

    @Mapping(target = "isActive", source = "active")
    ProfessionalEntity toEntity(Professional domain);
}