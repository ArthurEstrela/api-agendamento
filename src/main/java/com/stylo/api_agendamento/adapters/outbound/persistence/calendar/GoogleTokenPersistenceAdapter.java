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
    public void saveTokens(UUID professionalId, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        // Busca a entidade existente para não perder o resourceId ou cria uma nova se não existir (Upsert)
        var entity = repository.findById(professionalId).orElseGet(() -> 
                GoogleTokenEntity.builder().professionalId(professionalId).build()
        );

        entity.setAccessToken(accessToken);
        entity.setExpiresAt(expiresAt);
        entity.setStatus(GoogleConnectionStatus.CONNECTED);

        // O Google geralmente só envia o refresh_token no primeiro login.
        // Se for uma renovação de token, não podemos apagar o antigo!
        if (refreshToken != null && !refreshToken.isBlank()) {
            entity.setRefreshToken(refreshToken);
        }

        repository.save(entity);
    }

    @Override
    @Transactional
    public void markAsDisconnected(UUID professionalId) {
        repository.findById(professionalId).ifPresent(entity -> {
            entity.setStatus(GoogleConnectionStatus.DISCONNECTED);
            repository.save(entity);
        });
    }

    @Override
    public Optional<TokenData> findByProfessionalId(UUID professionalId) {
        return repository.findById(professionalId)
                .map(e -> new TokenData(
                        e.getAccessToken(), 
                        e.getRefreshToken(), 
                        e.getExpiresAt(),
                        e.getStatus()
                ));
    }

    @Override
    public boolean isTokenExpiringSoon(UUID professionalId) {
        return repository.findById(professionalId)
                // Retorna true se expirar em menos de 5 minutos
                .map(token -> token.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5)))
                // Se não existir token no banco, tecnicamente precisa renovar/criar
                .orElse(true); 
    }
}