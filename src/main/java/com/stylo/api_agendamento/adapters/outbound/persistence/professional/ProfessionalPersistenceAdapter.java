package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Optional<Professional> findById(String id) {
        return jpaProfessionalRepository.findById(UUID.fromString(id))
                .map(professionalMapper::toDomain);
    }

    @Override
    public List<Professional> findAllByProviderId(String providerId) {
        return jpaProfessionalRepository.findAllByServiceProviderId(UUID.fromString(providerId))
                .stream()
                .map(professionalMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Professional> findByIdWithLock(String id) {
        return jpaProfessionalRepository.findByIdWithLock(UUID.fromString(id))
                .map(professionalMapper::toDomain);
    }
}