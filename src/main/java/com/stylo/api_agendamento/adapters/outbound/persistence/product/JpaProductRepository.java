package com.stylo.api_agendamento.adapters.outbound.persistence.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {

    Page<ProductEntity> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);

    List<ProductEntity> findByServiceProviderIdAndIsActiveTrue(UUID serviceProviderId);

    List<ProductEntity> findAllByIdIn(List<UUID> ids);

    // ✨ CORREÇÃO: Os nomes devem bater com os campos da ProductEntity (stockQuantity e minStockAlert)
    @Query("SELECT p FROM ProductEntity p WHERE p.serviceProviderId = :providerId AND p.stockQuantity <= p.minStockAlert")
    List<ProductEntity> findLowStockProducts(@Param("providerId") UUID providerId);
}