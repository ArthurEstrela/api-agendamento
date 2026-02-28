package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.RemunerationType;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateProfessionalUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    
    // ✨ MUDANÇA AQUI: Injetamos o UserRepository em vez do ServiceProviderRepository
    private final IUserRepository userRepository; 

    @Transactional
    public Professional execute(Input input) {
        log.info("Iniciando criação de profissional para o estabelecimento ID: {}", input.providerId());

        // 1. Resolve o E-mail (Se vier vazio do front, gera um fictício para o Staff)
        String emailToUse = (input.email() == null || input.email().isBlank())
                ? "staff_" + UUID.randomUUID().toString().substring(0, 8) + "@stylo.com"
                : input.email();

        // 2. Verifica se o profissional sendo criado é o Dono
        // Buscamos se existe um usuário com esse e-mail e se ele tem o nível de dono (SERVICE_PROVIDER)
        boolean isCreatingOwner = userRepository.findByEmail(emailToUse)
                .map(user -> UserRole.SERVICE_PROVIDER.equals(user.getRole()))
                .orElse(false);

        // 3. Regra de Negócio: Bloquear duplicação de perfil do Dono
        if (isCreatingOwner) {
            boolean ownerAlreadyExists = professionalRepository.findByServiceProviderId(input.providerId())
                    .stream()
                    .anyMatch(p -> p.isOwner() && p.isActive()); // Bloqueia apenas se já houver um dono ATIVO

            if (ownerAlreadyExists) {
                log.warn("Tentativa de criar múltiplos perfis de dono para o estabelecimento: {}", input.providerId());
                throw new BusinessException("Você já possui um perfil de profissional ativo no sistema.");
            }
        }

        // 4. Busca os serviços selecionados
        List<Service> services = new ArrayList<>();
        if (input.serviceIds() != null && !input.serviceIds().isEmpty()) {
            services = serviceRepository.findAllByIds(input.serviceIds());
        }

        // 5. Cria a Entidade passando a flag isOwner correta
        Professional professional = Professional.create(
                input.name(),
                emailToUse,
                input.providerId(),
                services,
                new ArrayList<>(), // Disponibilidade pode ser configurada depois
                isCreatingOwner    // ✨ PASSA A FLAG DINÂMICA AQUI
        );

        // 6. Atualiza dados opcionais (Bio e Comissão)
        if (input.bio() != null) {
            professional.updateProfile(input.name(), input.bio(), null);
        }

        if (input.commissionPercentage() != null && input.commissionPercentage().compareTo(BigDecimal.ZERO) > 0) {
            professional.updateCommissionSettings(RemunerationType.PERCENTAGE, input.commissionPercentage());
        }

        log.info("Profissional criado com sucesso. ID: {}, É dono: {}", professional.getId(), isCreatingOwner);
        return professionalRepository.save(professional);
    }

    public record Input(
            UUID providerId,
            String name,
            String email,
            String bio,
            BigDecimal commissionPercentage,
            List<UUID> serviceIds
    ) {}
}