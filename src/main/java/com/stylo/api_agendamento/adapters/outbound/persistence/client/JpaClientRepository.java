package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientRepository extends JpaRepository<ClientEntity, UUID> {

    Optional<ClientEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<ClientEntity> findById(UUID id);

    // ✨ MAGIA DO MARKETPLACE: O cliente é global, mas o salão SÓ VÊ clientes 
    // que já fizeram pelo menos um agendamento com ele (tabela AppointmentEntity).
    @Query("""
            SELECT DISTINCT c FROM ClientEntity c
            WHERE EXISTS (
                SELECT 1 FROM AppointmentEntity a 
                WHERE a.clientId = c.id AND a.serviceProviderId = :providerId
            )
            AND (:nameFilter IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :nameFilter, '%')))
           """)
    Page<ClientEntity> findClientsByProviderInteraction(
            @Param("providerId") UUID providerId,
            @Param("nameFilter") String nameFilter,
            Pageable pageable);
}