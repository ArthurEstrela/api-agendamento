package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("Token de recuperação é obrigatório.");
        }

        // 1. Busca o usuário pelo token
        User user = userRepository.findByResetPasswordToken(token)
                .orElseThrow(() -> new BusinessException("Token inválido ou já utilizado."));

        // 2. Valida expiração (Regra de Domínio)
        if (!user.isResetTokenValid()) {
            throw new BusinessException("Este link de recuperação expirou. Solicite um novo.");
        }

        // 3. Atualiza a senha com Hash seguro
        // O método updatePassword do domínio deve limpar o token após a troca
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.changePassword(encodedPassword);
        
        userRepository.save(user);
        
        // 4. Poderia disparar evento: PasswordChangedEvent (para avisar o usuário por e-mail)
    }
}