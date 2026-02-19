package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;

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
    boolean existsByDocument(Document document); // CPF/CNPJ único
    boolean existsBySlug(String slugValue);

    // --- JOBS DE ASSINATURA (SAAS) ---
    
    /**
     * Busca estabelecimentos cujo período de teste acabou hoje.
     */
    List<ServiceProvider> findExpiredTrials(LocalDateTime threshold);

    /**
     * Busca estabelecimentos cujo período de carência (Grace Period) acabou.
     */
    List<ServiceProvider> findExpiredGracePeriods(LocalDateTime threshold);

    /**
     * Busca assinaturas que vão vencer em breve (para enviar emails de aviso).
     */
    List<ServiceProvider> findUpcomingExpirations(LocalDateTime threshold);
}