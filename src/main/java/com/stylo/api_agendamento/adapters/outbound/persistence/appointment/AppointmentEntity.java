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
@Builder(toBuilder = true)
public class AppointmentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // --- DADOS DO CLIENTE (Snapshots) ---
    @Column(name = "client_id")
    private UUID clientId; // Nullable para Walk-ins (clientes sem conta)
    
    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_email")
    private String clientEmail; // ✨ Essencial para notificações e e-mails de lembrete

    @Column(name = "client_phone")
    private String clientPhone;

    // --- DADOS DO ESTABELECIMENTO E PROFISSIONAL ---
    @Column(nullable = false, name = "service_provider_id")
    private UUID serviceProviderId; // ✨ Renomeado para bater exato com o Domínio

    @Column(name = "business_name")
    private String businessName; // ✨ Snapshot do nome do salão (Ex: "Barbearia Stylo")

    @Column(nullable = false, name = "professional_id")
    private UUID professionalId;

    @Column(name = "professional_name")
    private String professionalName; // Snapshot

    // --- SERVIÇOS (Catálogo) ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "appointment_services", 
            joinColumns = @JoinColumn(name = "appointment_id"), 
            inverseJoinColumns = @JoinColumn(name = "service_id"))
    @Builder.Default
    private List<ServiceEntity> services = new ArrayList<>();

    // --- TEMPO E AGENDA ---
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
    private String externalEventId; // Mantém String (ID do Google Calendar não é UUID)

    // --- FINANCEIRO (Com precisão garantida) ---
    @Column(name = "total_price", precision = 19, scale = 2)
    private BigDecimal totalPrice; 

    @Column(name = "final_price", precision = 19, scale = 2)
    private BigDecimal finalPrice; 

    @Column(name = "coupon_id")
    private UUID couponId; // ✨ Corrigido de String para UUID

    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;

    // --- COMISSÕES ---
    @Column(name = "professional_commission", precision = 19, scale = 2)
    private BigDecimal professionalCommission;

    @Column(name = "service_provider_fee", precision = 19, scale = 2)
    private BigDecimal serviceProviderFee; 

    @Column(name = "commission_settled")
    private boolean commissionSettled;

    // --- DETALHES ---
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // --- LEMBRETES E NOTIFICAÇÕES ---
    @Column(name = "reminder_minutes")
    private Integer reminderMinutes;

    @Column(name = "reminder_sent")
    private boolean reminderSent; // ✨ Corrigido de 'notified' para mapear com o markReminderAsSent()

    @Column(nullable = false, name = "is_personal_block")
    private boolean isPersonalBlock;

    // --- CANCELAMENTO ---
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private UUID cancelledBy; // ✨ Corrigido para UUID (quem cancelou)

    // --- ITENS DA COMANDA (PRODUTOS FÍSICOS) ---
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "appointment_id") 
    @Builder.Default
    private List<AppointmentItemEntity> items = new ArrayList<>();
    
    // --- CALLBACKS JPA ---
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.discountAmount == null) this.discountAmount = BigDecimal.ZERO;
        if (this.totalPrice == null) this.totalPrice = BigDecimal.ZERO;
        if (this.finalPrice == null) this.finalPrice = BigDecimal.ZERO;
    }
}