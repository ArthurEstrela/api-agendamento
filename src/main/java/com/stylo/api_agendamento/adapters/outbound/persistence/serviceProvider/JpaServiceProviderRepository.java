package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaServiceProviderRepository extends JpaRepository<ServiceProviderEntity, UUID> {
    Optional<ServiceProviderEntity> findByPublicProfileSlug(String slug);

    boolean existsByDocumentValue(String value);

    boolean existsByPublicProfileSlug(String slug);

    @Query("SELECT s FROM ServiceProviderEntity s WHERE s.subscriptionStatus = 'TRIAL' AND s.trialEndsAt < :now")
    List<ServiceProviderEntity> findExpiredTrials(@Param("now") LocalDateTime now);

    List<ServiceProviderEntity> findByPublicProfileSlugIsNotNull();
}