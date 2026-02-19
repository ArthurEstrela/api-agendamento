package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "professional_availabilities", indexes = {
    @Index(name = "idx_avail_prof_day", columnList = "professional_id, day_of_week")
})
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class DailyAvailabilityEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_open", nullable = false)
    private boolean isOpen;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    // Adição estratégica: Intervalo de Almoço/Pausa
    @Column(name = "break_start")
    private LocalTime breakStart;

    @Column(name = "break_end")
    private LocalTime breakEnd;
}