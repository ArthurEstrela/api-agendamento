package com.stylo.api_agendamento.core.domain;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TimeSlot {
    private String start; // HH:mm
    private String end;   // HH:mm
}