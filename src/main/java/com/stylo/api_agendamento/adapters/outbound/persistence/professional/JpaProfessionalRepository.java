package com.stylo.api_agendamento.adapters.outbound.persistence.professional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface JpaProfessionalRepository extends JpaRepository<ProfessionalEntity, UUID> {
    Optional<ProfessionalEntity> findByEmail(String email);

    List<ProfessionalEntity> findAllByServiceProviderId(UUID providerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProfessionalEntity p WHERE p.id = :id")
    Optional<ProfessionalEntity> findByIdWithLock(@Param("id") UUID id);
}