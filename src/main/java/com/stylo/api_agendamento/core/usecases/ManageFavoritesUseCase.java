package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IClientRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManageFavoritesUseCase {

    private final IClientRepository clientRepository;
    private final IServiceProviderRepository providerRepository;

    @Transactional
    public void addFavorite(UUID clientId, UUID providerId) {
        // 1. Busca o cliente
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado."));

        // 2. Valida se o estabelecimento (Provider) realmente existe antes de favoritar
        providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));
                
        // 3. Delega a ação para o Domínio (Clean Architecture)
        client.addFavoriteProvider(providerId);

        // 4. Persiste o estado. O JPA fará o INSERT na tabela de junção automaticamente
        clientRepository.save(client);
    }

    @Transactional
    public void removeFavorite(UUID clientId, UUID providerId) {
        // 1. Busca o cliente
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado."));

        // 2. Delega a ação para o Domínio
        // Nota: Não precisamos verificar se o Provider existe aqui. 
        // Se ele não existir ou já tiver sido apagado, simplesmente removemos o ID da lista do cliente.
        client.removeFavoriteProvider(providerId);

        // 3. Persiste o estado. O JPA fará o DELETE na tabela de junção automaticamente
        clientRepository.save(client);
    }
}