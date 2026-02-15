package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RequestPasswordResetUseCase {

    private final IUserRepository userRepository;
    private final INotificationProvider notificationProvider;

    public void execute(String email) {
        // Segurança: Sempre retornamos 200/OK mesmo se o e-mail não existir
        // para evitar "User Enumeration Attacks".
        userRepository.findByEmail(email).ifPresent(user -> {
            user.generatePasswordResetToken();
            userRepository.save(user);

            String link = "https://stylo.app.br/reset-password?token=" + user.getResetPasswordToken();
            
            try {
                notificationProvider.sendPasswordResetEmail(user.getEmail(), user.getName(), link);
            } catch (Exception e) {
                log.error("Erro ao enviar email de reset para {}: {}", email, e.getMessage());
            }
        });
    }
}