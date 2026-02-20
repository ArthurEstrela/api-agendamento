package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.usecases.dto.ProviderSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchServiceProvidersUseCase {

    private final IServiceProviderRepository providerRepository;

    public PagedResult<ServiceProvider> execute(ProviderSearchCriteria criteria, int page, int size) {
        // Aqui você pode adicionar regras de negócio se necessário 
        // (Ex: se o minRating for passado negativo, corrigir para 0)
        return providerRepository.searchProviders(criteria, page, size);
    }
}