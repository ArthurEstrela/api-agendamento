package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.usecases.dto.ProviderSearchCriteria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IServiceProviderRepository {

    ServiceProvider save(ServiceProvider provider);

    Optional<ServiceProvider> findById(UUID id);

    /**
     * Busca estabelecimento pela URL amigável (ex: stylo.com/barbearia-top).
     */
    Optional<ServiceProvider> findBySlug(Slug slug);

    // --- VALIDAÇÕES DE UNICIDADE ---
    boolean existsByDocument(Document document);

    boolean existsBySlug(String slugValue);

    // --- JOBS DE ASSINATURA E SITEMAP ---

    /**
     * Lista todos os estabelecimentos que possuem perfil público configurado.
     */
    List<ServiceProvider> findAllWithPublicProfile();

    /**
     * Busca estabelecimentos cujo período de teste acabou hoje.
     */
    List<ServiceProvider> findExpiredTrials(LocalDateTime threshold);

    /**
     * Busca estabelecimentos cujo período de carência (Grace Period) acabou.
     */
    List<ServiceProvider> findExpiredGracePeriods(LocalDateTime threshold);

    /**
     * Busca assinaturas que vão vencer em breve.
     */
    List<ServiceProvider> findUpcomingExpirations(LocalDateTime threshold);

    PagedResult<ServiceProvider> getFavoriteProvidersByClient(UUID clientId, int page, int size);

    PagedResult<ServiceProvider> searchProviders(ProviderSearchCriteria criteria, int page, int size);
}