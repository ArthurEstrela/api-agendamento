package com.stylo.api_agendamento.adapters.inbound.rest.dto.professional;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty; // ✨ IMPORTANTE
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record DailyAvailabilityRequest(
        @NotNull DayOfWeek dayOfWeek,

        @NotNull @JsonProperty("isWorkingDay") // ✨ Lê "isWorkingDay" do JSON do React
        Boolean isOpen, // ✨ Mas chama "isOpen" aqui no Java para bater com o domínio

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime startTime,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime endTime) {
}