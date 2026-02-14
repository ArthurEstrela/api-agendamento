package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "payouts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayoutEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID professionalId;

    @Column(nullable = false)
    private UUID serviceProviderId;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    private String status;

    @ElementCollection
    @CollectionTable(name = "payout_appointments", joinColumns = @JoinColumn(name = "payout_id"))
    @Column(name = "appointment_id")
    private List<UUID> appointmentIds; // IDs dos agendamentos que compõem este repasse
}