package com.stylo.api_agendamento.core.domain;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {
    private String id;
    private String clientId;
    private String clientName;
    private String clientPhone; 
    private String providerId;
    private String professionalId;
    private String professionalName;
    private List<Service> services;
    private String serviceName; 
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalDuration; // Em minutos
    private AppointmentStatus status;
    private String paymentMethod; // "pix" | "credit_card" | "cash"
    private BigDecimal totalPrice;
    private BigDecimal finalPrice;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public void cancel() {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Não é possível cancelar um agendamento já finalizado.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }
}