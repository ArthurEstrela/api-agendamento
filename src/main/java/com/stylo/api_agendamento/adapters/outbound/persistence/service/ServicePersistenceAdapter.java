package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Service> findById(UUID id) {
        return jpaServiceRepository.findById(id)
                .map(serviceMapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        jpaServiceRepository.deleteById(id);
    }

    @Override
    public List<Service> findAllByIds(List<UUID> ids) {
        return jpaServiceRepository.findAllById(ids)
                .stream()
                .map(serviceMapper::toDomain)
                .toList(); // Java 16+
    }

    @Override
    public List<Service> findAllByProviderId(UUID providerId) {
        return jpaServiceRepository.findAllByServiceProviderId(providerId)
                .stream()
                .map(serviceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Service> findAllActiveByProviderId(UUID providerId) {
        return jpaServiceRepository.findAllByServiceProviderIdAndIsActiveTrue(providerId)
                .stream()
                .map(serviceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Service> findAll() {
        return jpaServiceRepository.findAll()
                .stream()
                .map(serviceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Service> findByCategoryId(UUID categoryId) {
        return jpaServiceRepository.findAllByCategoryId(categoryId)
                .stream()
                .map(serviceMapper::toDomain)
                .toList();
    }
}