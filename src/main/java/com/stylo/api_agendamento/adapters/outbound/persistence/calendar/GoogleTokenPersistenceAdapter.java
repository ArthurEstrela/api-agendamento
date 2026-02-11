// src/main/java/com/stylo/api_agendamento/adapters/outbound/persistence/calendar/GoogleTokenPersistenceAdapter.java
package com.stylo.api_agendamento.adapters.outbound.persistence.calendar;

import com.stylo.api_agendamento.core.ports.IGoogleTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleTokenPersistenceAdapter implements IGoogleTokenRepository {

    private final JpaGoogleTokenRepository repository;

    @Override
    public void saveTokens(String professionalId, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        var entity = GoogleTokenEntity.builder()
                .professionalId(UUID.fromString(professionalId))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .build();
        repository.save(entity);
    }

    @Override
    public Optional<TokenData> findByProfessionalId(String professionalId) {
        return repository.findById(UUID.fromString(professionalId))
                .map(e -> new TokenData(e.getAccessToken(), e.getRefreshToken(), e.getExpiresAt()));
    }
}