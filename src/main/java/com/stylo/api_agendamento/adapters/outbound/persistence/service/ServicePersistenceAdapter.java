package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServicePersistenceAdapter implements IServiceRepository {

    private final JpaServiceRepository jpaServiceRepository;
    private final ServiceMapper serviceMapper;

    @Override
    public Service save(Service service) {
        var entity = serviceMapper.toEntity(service);
        var savedEntity = jpaServiceRepository.save(entity);
        return serviceMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Service> findById(String id) {
        return jpaServiceRepository.findById(UUID.fromString(id))
                .map(serviceMapper::toDomain);
    }

    @Override
    public void delete(String id) {
        jpaServiceRepository.deleteById(UUID.fromString(id));
    }

    @Override
    public List<Service> findAllByIds(List<String> ids) {
        List<UUID> uuids = ids.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
        return jpaServiceRepository.findAllById(uuids)
                .stream()
                .map(serviceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Service> findAllByProviderId(String providerId) {
        return jpaServiceRepository.findAllByServiceProviderId(UUID.fromString(providerId))
                .stream()
                .map(serviceMapper::toDomain)
                .collect(Collectors.toList());
    }

    // Implementação do método findAll() requisitado pelo erro
    @Override
    public List<Service> findAll() {
        return jpaServiceRepository.findAll()
                .stream()
                .map(serviceMapper::toDomain)
                .collect(Collectors.toList());
    }

    // Implementação do método findByCategoryId(String) requisitado pelo erro
    @Override
    public List<Service> findByCategoryId(String categoryId) {
        // Assume-se que o JpaServiceRepository possua um método de busca por categoria
        return jpaServiceRepository.findAllByCategoryId(UUID.fromString(categoryId))
                .stream()
                .map(serviceMapper::toDomain)
                .collect(Collectors.toList());
    }
}