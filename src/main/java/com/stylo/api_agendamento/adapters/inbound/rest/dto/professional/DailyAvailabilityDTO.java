package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record DailyAvailabilityDTO(
        DayOfWeek dayOfWeek,
        boolean isAvailable,

        // A anotação @JsonFormat garante que o Spring Boot envie as horas como "09:00" em vez de "09:00:00"
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") 
        LocalTime startTime,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") 
        LocalTime endTime
) {

    // O método fica dentro das chaves do record
    public static DailyAvailabilityDTO fromDomain(com.stylo.api_agendamento.core.domain.vo.DailyAvailability domain) {
        if (domain == null) return null;
        return new DailyAvailabilityDTO(
            domain.dayOfWeek(),
            domain.isOpen(), // Nota: O domínio utiliza "isOpen", mas o frontend espera "isAvailable"
            domain.startTime(),
            domain.endTime()
        );
    }
}