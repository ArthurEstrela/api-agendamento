package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaPayoutRepository extends JpaRepository<PayoutEntity, UUID> {
}