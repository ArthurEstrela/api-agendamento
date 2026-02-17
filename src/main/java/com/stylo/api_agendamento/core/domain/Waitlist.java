package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Waitlist extends BaseEntity {
    
    private String id;
    private String professionalId;
    private String clientId;
    private String clientName;
    private String clientPhone; // Para SMS/WhatsApp
    private String clientEmail;
    
    private LocalDate desiredDate; // O dia que ele quer
    private LocalDateTime requestTime; // Quando ele entrou na fila
    
    private boolean notified; // Se já avisamos ele
    private LocalDateTime notifiedAt;

    public void markAsNotified() {
        this.notified = true;
        this.notifiedAt = LocalDateTime.now();
    }
}