package com.stylo.api_agendamento.core.ports;

import java.util.List;

import com.stylo.api_agendamento.core.domain.Service;

public interface IServiceRepository {
    Service save(Service service);
    List<Service> findAllByProviderId(String providerId);
    void delete(String id);
}