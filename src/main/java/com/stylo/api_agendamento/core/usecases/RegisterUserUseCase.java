package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final IUserRepository userRepository;

    public User execute(User user) {
        // 1. Validação de Email Único
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new BusinessException("Este e-mail já está cadastrado no Stylo.");
        }

        // 2. Regra de Negócio: Definição de Data de Criação
        user.setCreatedAt(LocalDateTime.now());

        // 3. Validação de Documento para Profissionais/Providers
        if (user.getRole() == UserRole.PROFESSIONAL || user.getRole() == UserRole.SERVICE_PROVIDER) {
            validateDocument(user);
        }

        // 4. Persistência via Porta (O Core não sabe que é Postgres)
        return userRepository.save(user);
    }

    private void validateDocument(User user) {
        // Aqui entraria a lógica de validar se o CPF ou CNPJ é válido
        // Para um SaaS robusto, não permitimos cadastro sem documento para prestadores
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty()) {
            throw new BusinessException("Telefone de contato é obrigatório para prestadores.");
        }
    }
}