package com.stylo.api_agendamento.adapters.outbound.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stylo.api_agendamento.core.domain.AppointmentStatus;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "appointments")
@Data
public class AppointmentEntity {
    @Id
    private String id;
    private String clientId;
    private String providerId;
    private String professionalId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;
    private BigDecimal totalPrice;
}