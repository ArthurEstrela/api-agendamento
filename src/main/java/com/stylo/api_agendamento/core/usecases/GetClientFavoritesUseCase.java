package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetClientFavoritesUseCase {

    private final IServiceProviderRepository providerRepository;

    public PagedResult<ServiceProvider> execute(UUID clientId, int page, int size) {
        return providerRepository.getFavoriteProvidersByClient(clientId, page, size);
    }
}