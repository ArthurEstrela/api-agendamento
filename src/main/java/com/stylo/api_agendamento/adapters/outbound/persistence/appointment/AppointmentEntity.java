package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceEntity;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID clientId; // Pode ser null em agendamentos manuais
    private String clientName;

    private String clientPhone; // Persiste apenas o valor do VO ClientPhone

    @Column(nullable = false)
    private UUID providerId;

    @Column(nullable = false)
    private UUID professionalId;

    private String professionalName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "appointment_services", joinColumns = @JoinColumn(name = "appointment_id"), inverseJoinColumns = @JoinColumn(name = "service_id"))
    private List<ServiceEntity> services;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "external_event_id")
    private String externalEventId;

    private BigDecimal totalPrice;
    private BigDecimal finalPrice;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private Integer reminderMinutes;
    private boolean notified;

    @Column(nullable = false)
    private boolean isPersonalBlock;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    private boolean commissionSettled;

    // AppointmentEntity.java
    @Column(precision = 10, scale = 2)
    private BigDecimal professionalCommission; // Parte do profissional

    @Column(precision = 10, scale = 2)
    private BigDecimal serviceProviderFee; // Lucro do salão (SaaS Client)

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "appointment_id") // Cria a chave estrangeira na tabela appointment_items
    @Builder.Default
    private List<AppointmentItemEntity> items = new ArrayList<>();

}