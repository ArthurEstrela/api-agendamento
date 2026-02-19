package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IProductRepository {
    
    Product save(Product product);

    Optional<Product> findById(UUID id);

    /**
     * Busca produtos para listagem administrativa (com paginação).
     */
    PagedResult<Product> findAllByProviderId(UUID providerId, int page, int size);

    /**
     * Busca produtos ativos para venda no PDV/Agendamento.
     */
    List<Product> findAllActiveByProviderId(UUID providerId);

    /**
     * Busca múltiplos produtos de uma vez (Otimização para checkout).
     */
    List<Product> findAllByIds(List<UUID> ids);

    /**
     * Busca produtos com estoque abaixo do mínimo configurado (para alertas).
     */
    List<Product> findLowStock(UUID providerId);
    
    void delete(UUID id);
}