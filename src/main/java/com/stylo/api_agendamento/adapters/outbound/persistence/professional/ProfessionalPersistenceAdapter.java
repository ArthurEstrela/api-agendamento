package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProfessionalPersistenceAdapter implements IProfessionalRepository {

    private final JpaProfessionalRepository jpaProfessionalRepository;
    private final ProfessionalMapper professionalMapper;

    @Override
    public Professional save(Professional professional) {
        var entity = professionalMapper.toEntity(professional);
        var savedEntity = jpaProfessionalRepository.save(entity);
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
                .toList(); // Java 16+
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