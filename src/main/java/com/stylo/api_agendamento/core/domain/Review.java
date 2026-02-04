package com.stylo.api_agendamento.core.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Review {
    private String id;
    private String appointmentId;
    private String clientId;
    private String clientName;
    private String serviceProviderId;
    private String professionalId;
    private String professionalName;
    private int rating; // 1 a 5
    private String comment;
    private LocalDateTime createdAt;
}