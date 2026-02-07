package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProfessionalRepository extends JpaRepository<ProfessionalEntity, UUID> {
    Optional<ProfessionalEntity> findByEmail(String email);
    List<ProfessionalEntity> findAllByServiceProviderId(UUID providerId);
}