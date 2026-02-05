package com.stylo.api_agendamento.core.domain;

import java.time.LocalTime;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyAvailability {
    private String dayOfWeek; // "MONDAY", "TUESDAY", etc.
    private boolean isOpen;   // Mudado de isAvailable para isOpen para clareza de negócio
    private LocalTime startTime; // Horário que começa a trabalhar
    private LocalTime endTime;   // Horário que para de trabalhar
}