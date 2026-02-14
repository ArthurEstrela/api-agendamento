package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.GoogleConnectionStatus;
import java.time.LocalDateTime;
import java.util.Optional;

public interface IGoogleTokenRepository {
    // Atualizado para incluir o Status
    void saveTokens(String professionalId, String accessToken, String refreshToken, LocalDateTime expiresAt);

    // Novo método para desconectar
    void markAsDisconnected(String professionalId);

    Optional<TokenData> findByProfessionalId(String professionalId);

    // Agora o TokenData retorna o status para o Adapter verificar antes de tentar
    // usar
    record TokenData(String accessToken, String refreshToken, LocalDateTime expiresAt, GoogleConnectionStatus status) {
    }
}