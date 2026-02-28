package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.usecases.dto.ProfessionalProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ListProfessionalsByProviderUseCase {

    private final IProfessionalRepository professionalRepository;
    
    // Injetamos o UseCase de perfil para reaproveitar a lógica de montagem do DTO 
    // (que já traz serviços, horários, foto, etc.)
    private final GetProfessionalProfileUseCase getProfessionalProfileUseCase;

    @Transactional(readOnly = true)
    public List<ProfessionalProfile> execute(UUID providerId) {
        log.info("Buscando lista de profissionais ativos para o estabelecimento ID: {}", providerId);

        // 1. Busca as entidades de domínio dos profissionais vinculados ao estabelecimento
        List<Professional> professionals = professionalRepository.findByServiceProviderId(providerId);

        // 2. Filtra apenas os ativos e mapeia cada um para o Perfil completo
        List<ProfessionalProfile> profiles = professionals.stream()
                .filter(p -> Boolean.TRUE.equals(p.isActive())) // Proteção contra null e garante apenas ativos
                .map(professional -> getProfessionalProfileUseCase.execute(professional.getId()))
                .collect(Collectors.toList());

        log.info("Encontrados {} profissionais ativos para o estabelecimento ID: {}", profiles.size(), providerId);

        return profiles;
    }
}