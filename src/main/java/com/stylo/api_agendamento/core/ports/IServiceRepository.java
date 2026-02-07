package com.stylo.api_agendamento.core.ports;

import java.util.List;
import java.util.Optional;

import com.stylo.api_agendamento.core.domain.Service;

public interface IServiceRepository {
    Service save(Service service);
    Optional<Service> findById(String id);
    void delete(String id);
    List<Service> findAllByIds(List<String> ids);
    List<Service> findAllByProviderId(String providerId);
    List<Service> findAll(); 
    List<Service> findByCategoryId(String categoryId);
}