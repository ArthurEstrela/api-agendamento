package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaServiceProviderRepository extends JpaRepository<ServiceProviderEntity, UUID> {

    Optional<ServiceProviderEntity> findByPublicProfileSlug(String slug);

    boolean existsByDocumentValue(String value);

    boolean existsByPublicProfileSlug(String slug);

    List<ServiceProviderEntity> findByPublicProfileSlugIsNotNull();

    @Query("SELECT s FROM ServiceProviderEntity s WHERE s.subscriptionStatus = 'GRACE_PERIOD' AND s.gracePeriodEndsAt <= :now")
    List<ServiceProviderEntity> findExpiredGracePeriods(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM ServiceProviderEntity s WHERE s.subscriptionStatus = 'ACTIVE' AND s.trialEndsAt <= :threshold")
    List<ServiceProviderEntity> findUpcomingExpirations(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT s FROM ServiceProviderEntity s WHERE s.subscriptionStatus = 'TRIAL' AND s.trialEndsAt <= :now")
    List<ServiceProviderEntity> findExpiredTrials(@Param("now") LocalDateTime now);
}