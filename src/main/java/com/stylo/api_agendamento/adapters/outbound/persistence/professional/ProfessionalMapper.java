package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper;
import com.stylo.api_agendamento.core.domain.Professional;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = { ServiceMapper.class })
public interface ProfessionalMapper {

    Professional toDomain(ProfessionalEntity entity);

    ProfessionalEntity toEntity(Professional domain);
}