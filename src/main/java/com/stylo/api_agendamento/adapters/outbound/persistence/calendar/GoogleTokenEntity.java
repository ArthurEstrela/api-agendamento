// src/main/java/com/stylo/api_agendamento/adapters/outbound/persistence/calendar/GoogleTokenEntity.java
package com.stylo.api_agendamento.adapters.outbound.persistence.calendar;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "professional_google_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenEntity {

    @Id
    private UUID professionalId; // PK é o próprio ID do profissional (1:1)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private String resourceId; // Para o Webhook/Watch
}