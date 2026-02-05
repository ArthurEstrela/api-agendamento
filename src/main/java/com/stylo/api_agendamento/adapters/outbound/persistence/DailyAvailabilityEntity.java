package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "professional_availabilities")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyAvailabilityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String professionalId; // FK para o profissional
    
    private String dayOfWeek; // "Monday", "Tuesday", etc. (conforme o types.ts)
    private boolean isAvailable;

    @ElementCollection
    @CollectionTable(
        name = "professional_availability_slots", 
        joinColumns = @JoinColumn(name = "availability_id")
    )
    private List<TimeSlotEntity> slots; // Lista de horários de início e fim
}