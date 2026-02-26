// src/main/java/com/stylo/api_agendamento/core/usecases/RegisterServiceProviderUseCase.java
package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.domain.vo.Address;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RegisterServiceProviderUseCase {

    private final IServiceProviderRepository providerRepository;
    private final IProfessionalRepository professionalRepository;
    private final IUserRepository userRepository;
    private final INotificationProvider notificationProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ServiceProvider execute(Input input) {
        if (providerRepository.existsByDocument(input.document())) {
            throw new BusinessException("Já existe um estabelecimento cadastrado com este documento.");
        }
        if (providerRepository.findBySlug(input.slug()).isPresent()) {
            throw new BusinessException("Esta URL amigável (Slug) já está em uso.");
        }
        if (userRepository.findByEmail(input.email()).isPresent()) {
            throw new BusinessException("Este e-mail já está vinculado a um usuário da plataforma.");
        }

        ServiceProvider provider = ServiceProvider.create(
                input.businessName(),
                input.document(),
                input.slug(),
                input.address(),
                input.email(),
                input.phone());

        ServiceProvider savedProvider = providerRepository.save(provider);

        User user = User.create(input.ownerName(), input.email(), input.phone(), UserRole.SERVICE_PROVIDER);
        user.changePassword(passwordEncoder.encode(input.password()));
        user.linkProvider(savedProvider.getId());

        // ✨ VINCULA O ID DO FIREBASE SE VIER NA REQUISIÇÃO (Igual fizemos no Cliente)
        if (input.firebaseUid() != null && !input.firebaseUid().isBlank()) {
            user.linkFirebase(input.firebaseUid());
        }

        userRepository.save(user);

        if (input.ownerIsProfessional()) {
            Professional ownerProfile = Professional.create(
                    input.ownerName(),
                    input.email(),
                    savedProvider.getId(),
                    new ArrayList<>(),
                    new ArrayList<>());

            professionalRepository.save(ownerProfile);

            // Opcional: Se quiser vincular o profissional ao user automaticamente
            // user.linkProfessional(ownerProfile.getId());
            // userRepository.save(user);
        }

        try {
            notificationProvider.sendWelcomeEmail(user.getEmail(), user.getName());
        } catch (Exception e) {
            log.warn("Falha não-bloqueante no envio de boas-vindas para {}: {}", user.getEmail(), e.getMessage());
        }

        log.info("Novo ServiceProvider cadastrado: {} | ID: {}", savedProvider.getBusinessName(),
                savedProvider.getId());
        return savedProvider;
    }

    public record Input(
            String businessName,
            Document document,
            Slug slug,
            Address address,
            String ownerName,
            String email,
            String password,
            String phone, // ✨ NOVO
            boolean ownerIsProfessional,
            String firebaseUid // ✨ NOVO
    ) {
    }
}