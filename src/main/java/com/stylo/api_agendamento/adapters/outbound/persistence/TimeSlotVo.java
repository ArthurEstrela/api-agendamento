package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.time.LocalTime;

@Embeddable
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class TimeSlotVo {

    @Column(name = "start_time", nullable = false)
    private LocalTime start;

    @Column(name = "end_time", nullable = false)
    private LocalTime end;
}