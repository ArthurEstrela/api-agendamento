package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {
    // O nome deve ser ServiceProviderId (maiusculo o I) para o JPA entender o mapeamento
    List<ExpenseEntity> findAllByServiceProviderIdAndDateBetween(UUID providerId, LocalDateTime start, LocalDateTime end);
}