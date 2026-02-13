package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder; // Use o do Spring Security

@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void execute(String token, String newPassword) {
        // Precisamos de um método: findByResetPasswordToken no repositório
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BusinessException("Token inválido ou expirado."));

        if (!user.isResetTokenValid(token)) {
            throw new BusinessException("Token expirado.");
        }

        // Atualiza a senha (já com Hash)
        User.withPassword(user, passwordEncoder.encode(newPassword));
        
        // Limpa o token para não ser usado de novo
        user.clearPasswordResetToken();
        
        userRepository.save(user);
    }
}