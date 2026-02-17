package com.stylo.api_agendamento.adapters.outbound.persistence.waitlist;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "waitlist", indexes = {
    @Index(name = "idx_waitlist_prof_date", columnList = "professional_id, desired_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaitlistEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "client_name")
    private String clientName;

    @Column(name = "client_phone")
    private String clientPhone;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "desired_date", nullable = false)
    private LocalDate desiredDate;

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    @Column(nullable = false)
    private boolean notified;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;
}