package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper;
import com.stylo.api_agendamento.adapters.outbound.persistence.mapper.AvailabilityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { ServiceMapper.class,
        AvailabilityMapper.class })
public interface ProfessionalMapper {

    Professional toDomain(ProfessionalEntity entity);

    ProfessionalEntity toEntity(Professional domain);
}