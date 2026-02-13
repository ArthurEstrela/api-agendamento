package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.usecases.dto.UpdateFcmTokenInput;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UpdateFcmTokenUseCase {

    private final IUserRepository userRepository;

    public void execute(UpdateFcmTokenInput input) {
        if (input.token() == null || input.token().isBlank()) {
            throw new BusinessException("Token inválido.");
        }

        User user = userRepository.findById(input.userId())
                .orElseThrow(() -> new BusinessException("Utilizador não encontrado."));

        user.updateFcmToken(input.token());
        userRepository.save(user);

        log.info("FCM Token atualizado com sucesso para o utilizador: {}", user.getId());
    }
}