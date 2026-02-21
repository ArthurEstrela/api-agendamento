package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaStockMovementRepository extends JpaRepository<StockMovementEntity, UUID> {

    // Busca movimentações de um produto específico (Histórico individual)
    Page<StockMovementEntity> findAllByProductId(UUID productId, Pageable pageable);

    // Busca todas as movimentações de um estabelecimento (Auditoria geral)
    Page<StockMovementEntity> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);
}