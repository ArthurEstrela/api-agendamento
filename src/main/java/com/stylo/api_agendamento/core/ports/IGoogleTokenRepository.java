package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.GoogleConnectionStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IGoogleTokenRepository {

    void saveTokens(UUID professionalId, String accessToken, String refreshToken, LocalDateTime expiresAt);

    void markAsDisconnected(UUID professionalId);

    Optional<TokenData> findByProfessionalId(UUID professionalId);

    /**
     * Verifica se o token precisa ser renovado (ex: expira em menos de 5 min).
     */
    boolean isTokenExpiringSoon(UUID professionalId);

    record TokenData(
        String accessToken, 
        String refreshToken, 
        LocalDateTime expiresAt, 
        GoogleConnectionStatus status
    ) {}
}