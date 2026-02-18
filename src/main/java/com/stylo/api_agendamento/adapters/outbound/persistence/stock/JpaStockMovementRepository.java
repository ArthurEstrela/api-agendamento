package com.stylo.api_agendamento.adapters.outbound.persistence.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaStockMovementRepository extends JpaRepository<StockMovementEntity, String> {
    // Métodos de busca personalizados podem ser adicionados aqui futuramente
}