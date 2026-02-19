package com.stylo.api_agendamento.adapters.outbound.persistence.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {

    // Listagem paginada para o painel administrativo
    Page<ProductEntity> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);

    // Listagem para a tela de venda/agendamento
    List<ProductEntity> findByServiceProviderIdAndIsActiveTrue(UUID serviceProviderId);

    // Busca produtos em lote (usado no checkout)
    List<ProductEntity> findAllByIdIn(List<UUID> ids);

    // Lógica de Estoque Baixo: quantidade atual <= nível de alerta
    @Query("SELECT p FROM ProductEntity p WHERE p.serviceProviderId = :providerId AND p.quantity <= p.lowStockAlert")
    List<ProductEntity> findLowStockProducts(@Param("providerId") UUID providerId);
}