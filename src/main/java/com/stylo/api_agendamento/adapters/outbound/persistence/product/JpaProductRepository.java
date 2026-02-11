package com.stylo.api_agendamento.adapters.outbound.persistence.product;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JpaProductRepository extends JpaRepository<ProductEntity, String> {
    // Para listar produtos na tela de agendamento (somente ativos)
    List<ProductEntity> findByServiceProviderIdAndIsActiveTrue(String serviceProviderId);
}