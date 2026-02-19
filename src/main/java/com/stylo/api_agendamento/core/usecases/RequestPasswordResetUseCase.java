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

    /**
     * Inicia o fluxo de recuperação de senha.
     * Implementa segurança contra User Enumeration.
     */
    public void execute(String email) {
        if (email == null || email.isBlank()) return;

        userRepository.findByEmail(email.toLowerCase().trim()).ifPresentOrElse(
            user -> {
                // O Domínio gera o token com expiração (ex: 1 hora)
                user.generatePasswordResetToken();
                userRepository.save(user);

                String resetLink = "https://stylo.app.br/reset-password?token=" + user.getResetPasswordToken();
                
                try {
                    notificationProvider.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);
                    log.info("Link de recuperação enviado com sucesso para o usuário.");
                } catch (Exception e) {
                    log.error("Falha ao enviar e-mail de recuperação para {}: {}", email, e.getMessage());
                }
            },
            () -> {
                // Simulamos um delay aleatório para evitar ataques de timing que 
                // revelariam que o e-mail não existe na base.
                try { Thread.sleep(100 + (long)(Math.random() * 200)); } catch (InterruptedException ignored) {}
                log.info("Solicitação de reset recebida para e-mail inexistente. Processo silenciado por segurança.");
            }
        );
    }
}