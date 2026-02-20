package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaClientRepository extends JpaRepository<ClientEntity, UUID> {

    Optional<ClientEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<ClientEntity> findByUserIdAndServiceProviderId(UUID userId, UUID serviceProviderId);

    // Mágica do JPA: Se o nomeFilter for null, ele ignora e traz todos.
    // Se não for null, faz um 'ILIKE' (ignorando maiúsculas e minúsculas).
    @Query("""
                SELECT c FROM ClientEntity c
                WHERE c.serviceProviderId = :providerId
                AND (:nameFilter IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :nameFilter, '%')))
            """)
    Page<ClientEntity> findAllByProviderIdAndNameFilter(
            @Param("providerId") UUID providerId,
            @Param("nameFilter") String nameFilter,
            Pageable pageable);
}