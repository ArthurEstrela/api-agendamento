package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;

@Entity
@Table(name = "payouts")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder(toBuilder = true)
public class PayoutEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "service_provider_id", nullable = false)
    private UUID serviceProviderId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    private String status;

    // ✨ Melhoria: Inicialização segura da lista para evitar NullPointerExceptions
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "payout_appointments", joinColumns = @JoinColumn(name = "payout_id"))
    @Column(name = "appointment_id")
    @Builder.Default
    private List<UUID> appointmentIds = new ArrayList<>(); 
}