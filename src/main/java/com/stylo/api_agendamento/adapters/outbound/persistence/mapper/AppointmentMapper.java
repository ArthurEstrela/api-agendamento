package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import com.stylo.api_agendamento.adapters.outbound.persistence.AppointmentEntity;
import com.stylo.api_agendamento.core.domain.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    // Converte do Domínio para a Entidade (para salvar no banco)
    AppointmentEntity toEntity(Appointment domain);

    // Converte da Entidade para o Domínio (para usar nos UseCases)
    Appointment toDomain(AppointmentEntity entity);
}