package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RegisterServiceProviderUseCase {

    private final IServiceProviderRepository providerRepository;
    private final IProfessionalRepository professionalRepository;
    private final IUserRepository userRepository;
    private final INotificationProvider notificationProvider; // ✨ Injetado

    public ServiceProvider execute(ServiceProviderInput input) {
        // 1. Validações
        if (providerRepository.existsByDocument(input.document())) {
            throw new BusinessException("Já existe um estabelecimento cadastrado com este documento.");
        }
        if (providerRepository.findBySlug(input.slug()).isPresent()) {
            throw new BusinessException("Esta URL amigável já está em uso.");
        }

        // 2. Criar Usuário
        User user = User.create(input.ownerName(), input.email(), UserRole.SERVICE_PROVIDER);
        User.withPassword(user, input.password());
        userRepository.save(user);

        // 3. Criar Estabelecimento
        ServiceProvider provider = ServiceProvider.create(
                input.businessName(),
                input.document(),
                input.slug(),
                input.address(),
                input.email());

        ServiceProvider savedProvider = providerRepository.save(provider);

        // 4. Perfil Profissional (Opcional)
        if (input.ownerIsProfessional()) {
            Professional ownerProfile = Professional.create(
                    input.ownerName(),
                    input.email(),
                    savedProvider.getId(),
                    new ArrayList<>(),
                    new ArrayList<>());
            professionalRepository.save(ownerProfile);
        }

        // 5. Boas-vindas (Assíncrono/Seguro)
        try {
            notificationProvider.sendWelcomeEmail(user.getEmail(), user.getName());
            log.info("E-mail de boas-vindas disparado para: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Erro no envio de boas-vindas: {}", e.getMessage());
            // Não bloqueia o cadastro se o e-mail falhar
        }

        return savedProvider;
    }

    public record ServiceProviderInput(
            String businessName,
            Document document,
            Slug slug,
            com.stylo.api_agendamento.core.domain.vo.Address address,
            String ownerName,
            String email,
            String password,
            boolean ownerIsProfessional) {
    }
}