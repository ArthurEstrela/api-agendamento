package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Product;
import java.util.List;
import java.util.Optional;

public interface IProductRepository {
    Product save(Product product);
    Optional<Product> findById(String id);
    List<Product> findByServiceProviderIdAndIsActiveTrue(String serviceProviderId);
}