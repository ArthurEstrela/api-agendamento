package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaServiceProviderRepository extends JpaRepository<ServiceProviderEntity, UUID>, JpaSpecificationExecutor<ServiceProviderEntity> {

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

    @Query(value = "SELECT sp.* FROM service_providers sp INNER JOIN client_favorite_providers cfp ON sp.id = cfp.provider_id WHERE cfp.client_id = :clientId", countQuery = "SELECT COUNT(*) FROM client_favorite_providers WHERE client_id = :clientId", nativeQuery = true)
    Page<ServiceProviderEntity> findFavoriteProvidersByClientId(@Param("clientId") UUID clientId, Pageable pageable);
}