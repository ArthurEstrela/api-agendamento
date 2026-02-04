package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.adapters.outbound.persistence.AppointmentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    // Converte Domínio para Entidade (JPA)
    AppointmentEntity toEntity(Appointment domain);

    // Converte Entidade (JPA) para Domínio
    Appointment toDomain(AppointmentEntity entity);
}