package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.core.domain.Professional;
import org.mapstruct.Mapper;

import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfessionalMapper {

    Professional toDomain(ProfessionalEntity entity);

    ProfessionalEntity toEntity(Professional domain);
}