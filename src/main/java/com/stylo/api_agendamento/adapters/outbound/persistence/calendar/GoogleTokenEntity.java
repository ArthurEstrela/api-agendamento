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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleTokenEntity {

    @Id
    private UUID professionalId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private String resourceId;

    // ✨ NOVO CAMPO
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private GoogleConnectionStatus status = GoogleConnectionStatus.CONNECTED;
}