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
    private String providerId;
    private String professionalId;
    private String serviceName;
    private List<Service> services;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;

    // Lógica de domínio: um agendamento só pode ser cancelado se não estiver finalizado
    public void cancel() {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Não é possível cancelar um agendamento já finalizado.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }
}