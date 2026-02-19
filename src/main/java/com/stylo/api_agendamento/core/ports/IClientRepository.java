package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Client;

import java.util.Optional;
import java.util.UUID;

public interface IClientRepository {

    Client save(Client client);

    Optional<Client> findById(UUID id);

    Optional<Client> findByEmail(String email);
    
    boolean existsByEmail(String email);

    void delete(UUID id);

    /**
     * Busca a ficha do cliente dentro de um estabelecimento específico.
     * (Um User global pode ter fichas diferentes em Providers diferentes).
     */
    Optional<Client> findByUserAndProvider(UUID userId, UUID serviceProviderId);

    /**
     * Lista clientes de um estabelecimento com paginação e filtro por nome.
     */
    PagedResult<Client> findAllByProviderId(UUID providerId, String nameFilter, int page, int size);
}