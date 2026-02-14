package com.stylo.api_agendamento.adapters.outbound.persistence.calendar;

import com.stylo.api_agendamento.core.domain.GoogleConnectionStatus;
import com.stylo.api_agendamento.core.ports.IGoogleTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleTokenPersistenceAdapter implements IGoogleTokenRepository {

    private final JpaGoogleTokenRepository repository;

    @Override
    @Transactional
    public void saveTokens(String professionalId, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        // Ao salvar tokens novos (login ou refresh), o status volta a ser CONNECTED
        var entity = GoogleTokenEntity.builder()
                .professionalId(UUID.fromString(professionalId))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .status(GoogleConnectionStatus.CONNECTED) // ✨ Força conectado
                .build();
        repository.save(entity);
    }

    @Override
    @Transactional
    public void markAsDisconnected(String professionalId) {
        repository.findById(UUID.fromString(professionalId)).ifPresent(entity -> {
            entity.setStatus(GoogleConnectionStatus.DISCONNECTED);
            repository.save(entity);
        });
    }

    @Override
    public Optional<TokenData> findByProfessionalId(String professionalId) {
        return repository.findById(UUID.fromString(professionalId))
                .map(e -> new TokenData(
                        e.getAccessToken(), 
                        e.getRefreshToken(), 
                        e.getExpiresAt(),
                        e.getStatus() // ✨ Mapeia o status
                ));
    }
}