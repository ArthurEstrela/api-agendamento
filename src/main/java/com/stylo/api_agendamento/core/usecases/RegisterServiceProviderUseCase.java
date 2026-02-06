package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

@RequiredArgsConstructor
public class RegisterServiceProviderUseCase {

    private final IServiceProviderRepository providerRepository;
    private final IProfessionalRepository professionalRepository;
    private final IUserRepository userRepository;

    public ServiceProvider execute(ServiceProviderInput input) {
        // 1. Segurança e Integridade: Validar se o documento (CPF/CNPJ) ou Slug já existem
        if (providerRepository.existsByDocument(input.document())) {
            throw new BusinessException("Já existe um estabelecimento cadastrado com este documento.");
        }

        if (providerRepository.findBySlug(input.slug()).isPresent()) {
            throw new BusinessException("Esta URL amigável já está em uso.");
        }

        // 2. Criar a Identidade de Acesso (User)
        User user = User.create(input.ownerName(), input.email(), UserRole.SERVICE_PROVIDER);
        User savedUser = userRepository.save(user);

        // 3. Criar o Estabelecimento (ServiceProvider)
        ServiceProvider provider = ServiceProvider.create(
            input.businessName(),
            input.document(),
            input.slug(),
            input.address()
        );
        ServiceProvider savedProvider = providerRepository.save(provider);

        // 4. Criar o Perfil de Profissional do Dono (Opcional, mas comum no Stylo)
        // Isso garante que o dono apareça na agenda imediatamente se ele for um prestador
        if (input.ownerIsProfessional()) {
            Professional ownerProfile = Professional.create(
                input.ownerName(),
                input.email(),
                savedProvider.getId(),
                new ArrayList<>(), // Inicia sem serviços, ele adicionará depois
                new ArrayList<>()  // Inicia sem disponibilidade, ele configurará
            );
            // Marcar como dono no perfil de profissional também
            // (Assumindo que adicionamos o setter ou usamos o builder no domínio)
            professionalRepository.save(ownerProfile);
        }

        return savedProvider;
    }

    // DTO de entrada para manter o Use Case limpo
    public record ServiceProviderInput(
        String businessName,
        Document document,
        Slug slug,
        com.stylo.api_agendamento.core.domain.vo.Address address,
        String ownerName,
        String email,
        boolean ownerIsProfessional
    ) {}
}