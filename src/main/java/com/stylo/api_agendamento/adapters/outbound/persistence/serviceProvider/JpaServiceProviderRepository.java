package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaServiceProviderRepository extends JpaRepository<ServiceProviderEntity, UUID> {
    Optional<ServiceProviderEntity> findByPublicProfileSlug(String slug);

    boolean existsByDocumentValue(String value);

    boolean existsByPublicProfileSlug(String slug);
}