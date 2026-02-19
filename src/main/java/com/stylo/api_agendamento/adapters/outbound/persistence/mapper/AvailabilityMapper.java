package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import com.stylo.api_agendamento.adapters.outbound.persistence.DailyAvailabilityEntity;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AvailabilityMapper {

    DailyAvailabilityEntity toEntity(DailyAvailability domain);
    
    // Mantido como default para garantir a imutabilidade do seu Value Object
    default DailyAvailability toDomain(DailyAvailabilityEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new DailyAvailability(
            entity.getDayOfWeek(),
            entity.isOpen(),
            entity.getStartTime(),
            entity.getEndTime()
        );
    }
}