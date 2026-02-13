package com.stylo.api_agendamento.core.ports;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;

public interface IServiceProviderRepository {
    ServiceProvider save(ServiceProvider provider);

    Optional<ServiceProvider> findById(String id);

    Optional<ServiceProvider> findBySlug(Slug slug);

    boolean existsByDocument(Document document);

    boolean existsBySlug(String slug);

    List<ServiceProvider> findExpiredTrials(LocalDateTime now);
}