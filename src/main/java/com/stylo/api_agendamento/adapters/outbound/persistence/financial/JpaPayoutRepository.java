package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Repository
public interface JpaPayoutRepository extends JpaRepository<PayoutEntity, UUID> {

    Page<PayoutEntity> findAllByProfessionalId(UUID professionalId, Pageable pageable);
}