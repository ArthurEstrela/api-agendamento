package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import com.stylo.api_agendamento.adapters.outbound.persistence.ProfessionalEntity;
import com.stylo.api_agendamento.core.domain.Professional;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ServiceMapper.class})
public interface ProfessionalMapper {
    ProfessionalEntity toEntity(Professional domain);
    Professional toDomain(ProfessionalEntity entity);
}