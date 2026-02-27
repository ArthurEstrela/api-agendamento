package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.RemunerationType;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class CreateProfessionalUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;

    @Transactional
    public Professional execute(Input input) {
        // Busca os serviços selecionados
        List<Service> services = new ArrayList<>();
        if (input.serviceIds() != null && !input.serviceIds().isEmpty()) {
            services = serviceRepository.findAllByIds(input.serviceIds());
        }

        // Se o email vier vazio do frontend (ex: staff que não usa o app), geramos um fictício para satisfazer a regra de negócio
        String email = (input.email() == null || input.email().isBlank()) 
                ? "staff_" + UUID.randomUUID().toString().substring(0,8) + "@stylo.com" 
                : input.email();

        Professional professional = Professional.create(
                input.name(),
                email,
                input.providerId(),
                services,
                new ArrayList<>() // Disponibilidade pode ser configurada depois
        );

        if (input.bio() != null) {
            professional.updateProfile(input.name(), input.bio(), null);
        }

        if (input.commissionPercentage() != null && input.commissionPercentage().compareTo(BigDecimal.ZERO) > 0) {
            professional.updateCommissionSettings(RemunerationType.PERCENTAGE, input.commissionPercentage());
        }

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