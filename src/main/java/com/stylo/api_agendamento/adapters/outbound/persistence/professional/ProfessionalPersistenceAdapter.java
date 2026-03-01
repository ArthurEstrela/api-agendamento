package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper; // ✨ 1. Import do ServiceMapper
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProfessionalPersistenceAdapter implements IProfessionalRepository {

    private final JpaProfessionalRepository jpaProfessionalRepository;
    private final ProfessionalMapper professionalMapper;
    private final ServiceMapper serviceMapper; // ✨ 2. Injeção do ServiceMapper aqui!

    @Override
    public Professional save(Professional professional) {
        // 1. Tenta achar a entidade que já existe no banco (para o Hibernate rastrear
        // as listas)
        ProfessionalEntity entity = jpaProfessionalRepository.findById(professional.getId())
                .orElseGet(() -> new ProfessionalEntity()); // Se for criação, cria nova

        // 2. Atualiza os dados básicos e de perfil na entidade
        entity.setId(professional.getId());
        entity.setServiceProviderId(professional.getServiceProviderId()); // Fundamental para não dar erro de Null
        entity.setName(professional.getName());
        entity.setEmail(professional.getEmail());
        entity.setBio(professional.getBio());
        entity.setAvatarUrl(professional.getAvatarUrl());
        entity.setActive(professional.isActive());

        // ✨ 3. CORREÇÕES CRÍTICAS: Mapeamento de regras de negócio e financeiro que
        // faltavam
        entity.setOwner(professional.isOwner()); // Resolve o bug de múltiplos perfis "dono"
        entity.setSlotInterval(professional.getSlotInterval());
        entity.setRemunerationType(professional.getRemunerationType());
        entity.setRemunerationValue(professional.getRemunerationValue());
        entity.setGatewayAccountId(professional.getGatewayAccountId());

        // ✨ 4. Mapeamento de Especialidades (Tags)
        if (entity.getSpecialties() == null) {
            entity.setSpecialties(new ArrayList<>());
        }
        entity.getSpecialties().clear();
        if (professional.getSpecialties() != null) {
            entity.getSpecialties().addAll(professional.getSpecialties());
        }

        // ✨ 5. A MÁGICA DOS SERVIÇOS ✨
        if (entity.getServices() == null) {
            entity.setServices(new ArrayList<>());
        }

        // Limpa as relações antigas na tabela professional_services
        entity.getServices().clear();

        // Nome padronizado para serviceEntities e proteção contra null pointer
        if (professional.getServices() != null) {
            List<ServiceEntity> serviceEntities = professional.getServices().stream()
                    .map(serviceMapper::toEntity)
                    .toList();

            // Insere os novos!
            entity.getServices().addAll(serviceEntities);
        }

        // 6. Salva a entidade
        ProfessionalEntity savedEntity = jpaProfessionalRepository.save(entity);

        // 7. Retorna o Domínio atualizado
        return professionalMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Professional> findById(UUID id) {
        return jpaProfessionalRepository.findById(id)
                .map(professionalMapper::toDomain);
    }

    @Override
    public Optional<Professional> findByEmail(String email) {
        return jpaProfessionalRepository.findByEmail(email)
                .map(professionalMapper::toDomain);
    }

    @Override
    public List<Professional> findAllByProviderId(UUID providerId) {
        return jpaProfessionalRepository.findAllByServiceProviderId(providerId)
                .stream()
                .map(professionalMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Professional> findByIdWithLock(UUID id) {
        return jpaProfessionalRepository.findByIdWithLock(id)
                .map(professionalMapper::toDomain);
    }

    @Override
    public Optional<Professional> findByGatewayAccountId(String gatewayAccountId) {
        return jpaProfessionalRepository.findByGatewayAccountId(gatewayAccountId)
                .map(professionalMapper::toDomain);
    }

    @Override
    public List<Professional> findByServiceProviderId(UUID providerId) {
        return jpaProfessionalRepository.findByServiceProviderId(providerId).stream()
                .map(professionalMapper::toDomain)
                .collect(Collectors.toList());
    }
}