package com.stylo.api_agendamento.adapters.outbound.persistence.review;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID appointmentId;

    @Column(nullable = false)
    private UUID clientId;
    private String clientName;

    @Column(nullable = false)
    private UUID serviceProviderId;

    @Column(nullable = false)
    private UUID professionalId;
    private String professionalName;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}