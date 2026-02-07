package com.stylo.api_agendamento.adapters.outbound.persistence.expense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {
    
    // Busca todas as despesas de um salão em um período específico
    List<ExpenseEntity> findAllByProviderIdAndDateBetween(UUID providerId, LocalDateTime start, LocalDateTime end);
}