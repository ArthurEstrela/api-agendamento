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
        // Extrai o valor String do Record Slug
        return jpaServiceProviderRepository.findByPublicProfileSlug(slug.value())
                .map(serviceProviderMapper::toDomain);
    }

    @Override
    public boolean existsByDocument(Document document) {
        // No seu JpaServiceProviderRepository você deve ter um método: 
        // boolean existsByDocumentValue(String value);
        return jpaServiceProviderRepository.existsByDocumentValue(document.value());
    }
}