package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    public Optional<ServiceProvider> findById(UUID id) {
        return jpaServiceProviderRepository.findById(id)
                .map(serviceProviderMapper::toDomain);
    }

    @Override
    public Optional<ServiceProvider> findBySlug(Slug slug) {
        return jpaServiceProviderRepository.findByPublicProfileSlug(slug.value())
                .map(serviceProviderMapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaServiceProviderRepository.existsByPublicProfileSlug(slug);
    }

    @Override
    public boolean existsByDocument(Document document) {
        return jpaServiceProviderRepository.existsByDocumentValue(document.value());
    }

    @Override
    public List<ServiceProvider> findExpiredTrials(LocalDateTime now) {
        return jpaServiceProviderRepository.findExpiredTrials(now)
                .stream()
                .map(serviceProviderMapper::toDomain)
                .toList();
    }

    @Override
    public List<ServiceProvider> findAllWithPublicProfile() {
        return jpaServiceProviderRepository.findByPublicProfileSlugIsNotNull()
                .stream()
                .map(serviceProviderMapper::toDomain)
                .toList();
    }

    @Override
    public List<ServiceProvider> findExpiredGracePeriods(LocalDateTime now) {
        return jpaServiceProviderRepository.findExpiredGracePeriods(now)
                .stream()
                .map(serviceProviderMapper::toDomain)
                .toList();
    }

    @Override
    public List<ServiceProvider> findUpcomingExpirations(LocalDateTime threshold) {
        return jpaServiceProviderRepository.findUpcomingExpirations(threshold)
                .stream()
                .map(serviceProviderMapper::toDomain)
                .toList();
    }

    @Override
    public PagedResult<ServiceProvider> getFavoriteProvidersByClient(UUID clientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ServiceProviderEntity> entityPage = jpaServiceProviderRepository.findFavoriteProvidersByClientId(clientId,
                pageable);

        List<ServiceProvider> domainList = entityPage.getContent().stream()
                .map(serviceProviderMapper::toDomain)
                .collect(Collectors.toList());

        return new PagedResult<>(
                domainList,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages());
    }
}