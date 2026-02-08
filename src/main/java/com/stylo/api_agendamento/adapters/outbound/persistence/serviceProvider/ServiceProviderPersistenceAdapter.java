package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceProviderPersistenceAdapter implements IServiceProviderRepository {

    private final JpaServiceProviderRepository jpaServiceProviderRepository;
    private final ServiceProviderMapper serviceProviderMapper;

    @Override
    public ServiceProvider save(ServiceProvider provider) {
        var entity = serviceProviderMapper.toEntity(provider);
        var savedEntity = jpaServiceProviderRepository.save(entity);
        return serviceProviderMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ServiceProvider> findById(String id) {
        return jpaServiceProviderRepository.findById(UUID.fromString(id))
                .map(serviceProviderMapper::toDomain);
    }

    @Override
    public Optional<ServiceProvider> findBySlug(Slug slug) {
        return jpaServiceProviderRepository.findByPublicProfileSlug(slug.value())
                .map(serviceProviderMapper::toDomain);
    }

    // Implementação do método que estava faltando
    @Override
    public boolean existsBySlug(String slug) {
        return jpaServiceProviderRepository.existsByPublicProfileSlug(slug);
    }

    @Override
    public boolean existsByDocument(Document document) {
        return jpaServiceProviderRepository.existsByDocumentValue(document.value());
    }
}