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
import org.springframework.security.crypto.password.PasswordEncoder; // Adicione isso se for codificar a senha

import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RegisterServiceProviderUseCase {

    private final IServiceProviderRepository providerRepository;
    private final IProfessionalRepository professionalRepository;
    private final IUserRepository userRepository;
    private final INotificationProvider notificationProvider;
    private final PasswordEncoder passwordEncoder; // ✨ Injetado para salvar a senha de forma segura

    @Transactional
    public ServiceProvider execute(Input input) {
        // 1. Validações de Unicidade
        if (providerRepository.existsByDocument(input.document())) {
            throw new BusinessException("Já existe um estabelecimento cadastrado com este documento.");
        }
        if (providerRepository.findBySlug(input.slug()).isPresent()) {
            throw new BusinessException("Esta URL amigável (Slug) já está em uso.");
        }
        if (userRepository.findByEmail(input.email()).isPresent()) {
            throw new BusinessException("Este e-mail já está vinculado a um usuário da plataforma.");
        }

        // 2. Criar Estabelecimento (Tenant)
        ServiceProvider provider = ServiceProvider.create(
                input.businessName(),
                input.document(),
                input.slug(),
                input.address(),
                input.email());

        ServiceProvider savedProvider = providerRepository.save(provider);

        // 3. Criar Usuário Dono (Vinculado ao Tenant)
        // ✨ CORREÇÃO 1: Passando uma String vazia "" ou nula para o telefone (3º parâmetro)
        User user = User.create(input.ownerName(), input.email(), "", UserRole.SERVICE_PROVIDER);
        
        // ✨ CORREÇÃO 2: Usamos changePassword com a senha já criptografada (melhor prática)
        user.changePassword(passwordEncoder.encode(input.password()));
        
        // ✨ CORREÇÃO 3: linkProvider é void, chamamos direto
        user.linkProvider(savedProvider.getId());
        
        userRepository.save(user);

        // 4. Perfil Profissional (Se o dono também atende clientes)
        if (input.ownerIsProfessional()) {
            Professional ownerProfile = Professional.create(
                    input.ownerName(),
                    input.email(),
                    savedProvider.getId(),
                    new ArrayList<>(), // Serviços vazios
                    new ArrayList<>()); // Disponibilidade vazia
            
            professionalRepository.save(ownerProfile);
        }

        // 5. Notificação de Boas-vindas
        try {
            notificationProvider.sendWelcomeEmail(user.getEmail(), user.getName());
        } catch (Exception e) {
            log.warn("Falha não-bloqueante no envio de boas-vindas para {}: {}", user.getEmail(), e.getMessage());
        }

        log.info("Novo ServiceProvider cadastrado: {} | ID: {}", savedProvider.getBusinessName(), savedProvider.getId());
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
            boolean ownerIsProfessional) {
    }
}