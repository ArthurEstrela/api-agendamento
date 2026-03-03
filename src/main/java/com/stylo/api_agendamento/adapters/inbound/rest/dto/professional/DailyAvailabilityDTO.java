package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty; // ✨ IMPORTANTE
import java.time.DayOfWeek;
import java.time.LocalTime;

public record DailyAvailabilityDTO(
        DayOfWeek dayOfWeek,

        @JsonProperty("isWorkingDay")
        boolean isWorkingDay,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime startTime,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime endTime) {
    public static DailyAvailabilityDTO fromDomain(com.stylo.api_agendamento.core.domain.vo.DailyAvailability domain) {
        if (domain == null)
            return null;
        return new DailyAvailabilityDTO(
                domain.dayOfWeek(),
                domain.isOpen(), // ✨ Pega o "isOpen" do domínio e joga no "isWorkingDay"
                domain.startTime(),
                domain.endTime());
    }
}