package com.stylo.api_agendamento.adapters.outbound.persistence.calendar;

import com.stylo.api_agendamento.core.domain.GoogleConnectionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "professional_google_tokens")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenEntity {

    // Relacionamento 1:1 direto com o Professional
    @Id
    @Column(name = "professional_id")
    private UUID professionalId;

    @Column(columnDefinition = "TEXT", nullable = false, name = "access_token")
    private String accessToken;

    @Column(columnDefinition = "TEXT", nullable = false, name = "refresh_token")
    private String refreshToken;

    @Column(nullable = false, name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "resource_id")
    private String resourceId; // Usado para Webhooks do Google (Push Notifications de Agenda)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GoogleConnectionStatus status = GoogleConnectionStatus.CONNECTED;
}