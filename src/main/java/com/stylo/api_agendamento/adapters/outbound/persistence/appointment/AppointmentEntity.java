package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
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
public class AppointmentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- DADOS DO CLIENTE ---
    @Column(name = "client_id")
    private UUID clientId; // Pode ser null em agendamentos manuais (Walk-in)
    
    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_phone")
    private String clientPhone; // Persiste apenas o valor do VO ClientPhone

    // --- DADOS DO PRESTADOR ---
    @Column(nullable = false, name = "provider_id")
    private UUID providerId;

    @Column(nullable = false, name = "professional_id")
    private UUID professionalId;

    @Column(name = "professional_name")
    private String professionalName;

    // --- SERVIÇOS ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "appointment_services", 
            joinColumns = @JoinColumn(name = "appointment_id"), 
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    private List<ServiceEntity> services;

    // --- TEMPO ---
    @Column(nullable = false, name = "start_time")
    private LocalDateTime startTime;

    @Column(nullable = false, name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "time_zone")
    private String timeZone;

    // --- STATUS E PAGAMENTO ---
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "external_event_id")
    private String externalEventId;

    // --- FINANCEIRO ---
    @Column(name = "total_price", precision = 19, scale = 2)
    private BigDecimal totalPrice; // Preço bruto (serviços + produtos)

    @Column(name = "final_price", precision = 19, scale = 2)
    private BigDecimal finalPrice; // Preço líquido (após descontos)

    // ✨ NOVO: Campos de Cupom
    @Column(name = "coupon_id", length = 36)
    private String couponId; // String para compatibilidade com CouponEntity

    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;

    // ✨ Comissões e Taxas
    @Column(name = "professional_commission", precision = 19, scale = 2)
    private BigDecimal professionalCommission; // Parte do profissional

    @Column(name = "service_provider_fee", precision = 19, scale = 2)
    private BigDecimal serviceProviderFee; // Lucro do salão

    @Column(name = "commission_settled")
    private boolean commissionSettled;

    // --- DETALHES ---
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reminder_minutes")
    private Integer reminderMinutes;

    private boolean notified;

    @Column(nullable = false, name = "is_personal_block")
    private boolean isPersonalBlock;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    // --- ITENS (PRODUTOS) ---
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "appointment_id") // Cria a chave estrangeira na tabela appointment_items
    @Builder.Default
    private List<AppointmentItemEntity> items = new ArrayList<>();
    
    // Callbacks do JPA para garantir dados consistentes
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }
    }
}