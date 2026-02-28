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

@Component
@RequiredArgsConstructor
public class ProfessionalPersistenceAdapter implements IProfessionalRepository {

    private final JpaProfessionalRepository jpaProfessionalRepository;
    private final ProfessionalMapper professionalMapper;
    private final ServiceMapper serviceMapper; // ✨ 2. Injeção do ServiceMapper aqui!

    @Override
    public Professional save(Professional professional) {
        // 1. Tenta achar a entidade que já existe no banco (para o Hibernate rastrear as listas)
        ProfessionalEntity entity = jpaProfessionalRepository.findById(professional.getId())
                .orElseGet(() -> new ProfessionalEntity()); // Se for criação, cria nova

        // 2. Atualiza os dados básicos na entidade
        entity.setId(professional.getId());
        entity.setServiceProviderId(professional.getServiceProviderId()); // Fundamental para não dar erro de Null
        entity.setName(professional.getName());
        entity.setEmail(professional.getEmail());
        entity.setBio(professional.getBio());
        entity.setAvatarUrl(professional.getAvatarUrl());
        entity.setActive(professional.isActive());
        
        // Pode adicionar outros campos básicos aqui (isOwner, remunerationType, etc) se precisar

        // 3. ✨ A MÁGICA DOS SERVIÇOS ✨
        if (entity.getServices() == null) {
            entity.setServices(new ArrayList<>());
        }
        
        // Limpa as relações antigas na tabela professional_services
        entity.getServices().clear(); 

        // ✨ 3. CORREÇÃO DA VARIÁVEL: Nome padronizado para serviceEntities
        List<ServiceEntity> serviceEntities = professional.getServices().stream()
                .map(serviceMapper::toEntity)
                .toList();

        // Insere os novos!
        entity.getServices().addAll(serviceEntities); 

        // 4. Salva a entidade
        ProfessionalEntity savedEntity = jpaProfessionalRepository.save(entity);

        // 5. Retorna o Domínio atualizado
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
}