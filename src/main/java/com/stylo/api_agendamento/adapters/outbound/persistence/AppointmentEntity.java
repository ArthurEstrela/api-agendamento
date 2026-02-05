package com.stylo.api_agendamento.adapters.outbound.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stylo.api_agendamento.core.domain.AppointmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;


@Entity
@Table(name = "appointments")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentEntity {
    @Id
    private String id;
    private String clientId;
    private String clientName;
    private String clientPhone; // Para o prestador ligar se precisar
    private String providerId;
    private String professionalId;
    private String professionalName;
    private String serviceName; 
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalDuration; 
    
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status; // PENDING, SCHEDULED, COMPLETED, CANCELLED
    
    private String paymentMethod; // "pix", "credit_card", "cash"
    private BigDecimal totalPrice;
    private BigDecimal finalPrice; // Valor com possível desconto
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}