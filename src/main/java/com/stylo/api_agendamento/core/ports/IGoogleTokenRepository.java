// src/main/java/com/stylo/api_agendamento/core/ports/IGoogleTokenRepository.java
package com.stylo.api_agendamento.core.ports;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IGoogleTokenRepository {
    void saveTokens(String professionalId, String accessToken, String refreshToken, LocalDateTime expiresAt);
    Optional<TokenData> findByProfessionalId(String professionalId);
    
    record TokenData(String accessToken, String refreshToken, LocalDateTime expiresAt) {}
}