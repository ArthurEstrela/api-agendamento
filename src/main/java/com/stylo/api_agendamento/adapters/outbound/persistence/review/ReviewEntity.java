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
@Builder(toBuilder = true)
public class ReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(name = "client_name")
    private String clientName;

    @Column(name = "service_provider_id", nullable = false)
    private UUID serviceProviderId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;
    
    @Column(name = "professional_name")
    private String professionalName;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // ✨ Callback para garantir que a data não seja esquecida no banco
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}