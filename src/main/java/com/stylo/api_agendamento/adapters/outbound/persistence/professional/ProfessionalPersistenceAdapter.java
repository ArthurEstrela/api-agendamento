package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.adapters.outbound.persistence.DailyAvailabilityEntity; // ✨ NOVO IMPORT
import com.stylo.api_agendamento.adapters.outbound.persistence.mapper.AvailabilityMapper; // ✨ NOVO IMPORT
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper;
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
    private final ServiceMapper serviceMapper;
    private final AvailabilityMapper availabilityMapper; // ✨ 1. Injeção do AvailabilityMapper aqui!

    @Override
    public Professional save(Professional professional) {
        // 1. Tenta achar a entidade que já existe no banco (para o Hibernate rastrear
        // as listas)
        ProfessionalEntity entity = jpaProfessionalRepository.findById(professional.getId())
                .orElseGet(() -> new ProfessionalEntity()); // Se for criação, cria nova

        // 2. Atualiza os dados básicos na entidade
        entity.setId(professional.getId());
        entity.setServiceProviderId(professional.getServiceProviderId());
        entity.setName(professional.getName());
        entity.setEmail(professional.getEmail());
        entity.setBio(professional.getBio());
        entity.setAvatarUrl(professional.getAvatarUrl());
        entity.setActive(professional.isActive());

        // ✨ Correções mantidas (Bug do Dono e do Financeiro)
        entity.setOwner(professional.isOwner());
        entity.setSlotInterval(professional.getSlotInterval());
        entity.setRemunerationType(professional.getRemunerationType());
        entity.setRemunerationValue(professional.getRemunerationValue());
        entity.setGatewayAccountId(professional.getGatewayAccountId());

        // Especialidades (Tags)
        if (entity.getSpecialties() == null) {
            entity.setSpecialties(new ArrayList<>());
        }
        entity.getSpecialties().clear();
        if (professional.getSpecialties() != null) {
            entity.getSpecialties().addAll(professional.getSpecialties());
        }

        // 3. A MÁGICA DOS SERVIÇOS
        if (entity.getServices() == null) {
            entity.setServices(new ArrayList<>());
        }
        entity.getServices().clear();
        if (professional.getServices() != null) {
            List<ServiceEntity> serviceEntities = professional.getServices().stream()
                    .map(serviceMapper::toEntity)
                    .toList();
            entity.getServices().addAll(serviceEntities);
        }

        // ✨ 4. A MÁGICA DA DISPONIBILIDADE (A CORREÇÃO DESSE BUG) ✨
        if (entity.getAvailability() == null) {
            entity.setAvailability(new ArrayList<>());
        }

        // Limpa os horários antigos do banco (orphanRemoval = true cuidará de
        // deletá-los)
        entity.getAvailability().clear();

        // Mapeia os novos horários que vieram do Domínio e joga na Entidade
        if (professional.getAvailability() != null) {
            List<DailyAvailabilityEntity> availabilityEntities = professional.getAvailability().stream()
                    .map(availability -> {
                        DailyAvailabilityEntity availEntity = availabilityMapper.toEntity(availability);
                        // ISSO É O QUE PREENCHE A COLUNA QUE O HIBERNATE ESTAVA DEIXANDO NULA
                        availEntity.setProfessionalId(professional.getId());
                        return availEntity;
                    })
                    .toList();
            entity.getAvailability().addAll(availabilityEntities);
        }

        // 5. Salva a entidade no banco de dados
        ProfessionalEntity savedEntity = jpaProfessionalRepository.save(entity);

        // 6. Retorna o Domínio atualizado
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