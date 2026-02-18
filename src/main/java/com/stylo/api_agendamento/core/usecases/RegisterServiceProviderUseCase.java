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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RegisterServiceProviderUseCase {

    private final IServiceProviderRepository providerRepository;
    private final IProfessionalRepository professionalRepository;
    private final IUserRepository userRepository;
    private final INotificationProvider notificationProvider;

    @Transactional // ✨ Importante: Garante que ou salva tudo (User + Provider + Link) ou nada.
    public ServiceProvider execute(ServiceProviderInput input) {
        // 1. Validações Prévias
        if (providerRepository.existsByDocument(input.document())) {
            throw new BusinessException("Já existe um estabelecimento cadastrado com este documento.");
        }
        if (providerRepository.findBySlug(input.slug()).isPresent()) {
            throw new BusinessException("Esta URL amigável já está em uso.");
        }
        if (userRepository.findByEmail(input.email()).isPresent()) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail.");
        }

        // 2. Criar Estabelecimento (Primeiro, para termos o ID)
        // Nota: A ordem pode variar dependendo da estratégia de ID, mas geralmente criamos o Provider
        // e depois vinculamos ao User, ou vice-versa com update. Aqui faremos o update no User.
        
        ServiceProvider provider = ServiceProvider.create(
                input.businessName(),
                input.document(),
                input.slug(),
                input.address(),
                input.email());

        ServiceProvider savedProvider = providerRepository.save(provider);

        // 3. Criar Usuário Dono
        User user = User.create(input.ownerName(), input.email(), UserRole.SERVICE_PROVIDER);
        user = User.withPassword(user, input.password());
        
        // ✨ O PULO DO GATO: Vincula o ID do Provider ao Usuário IMEDIATAMENTE
        user = user.linkProvider(savedProvider.getId());
        
        userRepository.save(user);

        // 4. Perfil Profissional (Se o dono também atende)
        if (input.ownerIsProfessional()) {
            Professional ownerProfile = Professional.create(
                    input.ownerName(),
                    input.email(),
                    savedProvider.getId(),
                    new ArrayList<>(),
                    new ArrayList<>());
            professionalRepository.save(ownerProfile);
        }

        // 5. Notificação de Boas-vindas (Falha segura)
        try {
            notificationProvider.sendWelcomeEmail(user.getEmail(), user.getName());
            log.info("E-mail de boas-vindas enviado para: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de boas-vindas: {}", e.getMessage());
            // Não relança a exceção para não cancelar o cadastro
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