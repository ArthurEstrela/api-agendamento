package com.stylo.api_agendamento.adapters.outbound.persistence.waitlist;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "waitlist", indexes = {
    @Index(name = "idx_waitlist_prof_date", columnList = "professional_id, desired_date"),
    @Index(name = "idx_waitlist_client", columnList = "client_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_name", nullable = false, length = 100)
    private String clientName;

    @Column(name = "client_phone", length = 20)
    private String clientPhone;

    @Column(name = "client_email", length = 150)
    private String clientEmail;

    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;

    @CreationTimestamp
    @Column(name = "request_time", nullable = false, updatable = false)
    private LocalDateTime requestTime;

    @Builder.Default
    @Column(nullable = false)
    private boolean notified = false;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
}