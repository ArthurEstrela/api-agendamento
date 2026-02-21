package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaExpenseRepository extends JpaRepository<ExpenseEntity, UUID> {

    // O nome correto (ServiceProviderId) garante que o JPA faça o mapeamento
    // automático
    List<ExpenseEntity> findAllByServiceProviderIdAndDateBetween(
            UUID serviceProviderId,
            LocalDateTime start,
            LocalDateTime end);

    // Adicione este método para suportar a listagem paginada por estabelecimento
    Page<ExpenseEntity> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);
}