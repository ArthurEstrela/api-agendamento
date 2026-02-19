package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class UpdateFcmTokenUseCase {

    private final IUserRepository userRepository;

    @Transactional
    public void execute(Input input) {
        if (input.token() == null || input.token().isBlank()) {
            throw new BusinessException("O token FCM não pode ser nulo ou vazio.");
        }

        User user = userRepository.findById(input.userId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));

        // Se o token for o mesmo, não faz nada (Evita IO desnecessário)
        if (input.token().equals(user.getFcmToken())) {
            return;
        }

        // ✨ Opcional: Garante que este token é único na base (segurança de dispositivo)
        userRepository.clearTokenIfInUse(input.token());

        user.updateFcmToken(input.token());
        userRepository.save(user);

        log.info("FCM Token atualizado para o usuário {} (Dispositivo vinculado).", user.getId());
    }

    public record Input(UUID userId, String token) {}
}