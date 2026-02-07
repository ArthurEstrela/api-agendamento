package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "professional_availabilities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyAvailabilityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    private boolean isOpen;

    private LocalTime startTime;

    private LocalTime endTime;
}