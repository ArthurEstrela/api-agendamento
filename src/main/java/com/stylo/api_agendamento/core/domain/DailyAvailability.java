package com.stylo.api_agendamento.core.domain;

import lombok.*;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyAvailability {
    private String dayOfWeek; // "Monday", "Tuesday", etc.
    private boolean isAvailable;
    private List<TimeSlot> slots;
}