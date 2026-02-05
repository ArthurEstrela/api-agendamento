package com.stylo.api_agendamento.adapters.outbound.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReviewEntity {
    @Id
    private String id;
    private String appointmentId;
    private String clientId;
    private String clientName;
    private String serviceProviderId;
    private String professionalId;
    private String professionalName;
    private Integer rating; // 1 a 5
    private String comment;
    private LocalDateTime createdAt;
}