package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlotEntity {
    private String startTime; // ex: "08:00"
    private String endTime;   // ex: "12:00"
}