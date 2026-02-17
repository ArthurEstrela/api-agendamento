package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Client;
import java.util.Optional;

public interface IClientRepository {

    /**
     * Salva ou atualiza um cliente no sistema.
     */
    Client save(Client client);

    /**
     * Busca um cliente pelo identificador único.
     */
    Optional<Client> findById(String id);

    /**
     * Busca um cliente pelo e-mail (usado em logins/validações).
     */
    Optional<Client> findByEmail(String email);

    /**
     * Remove um cliente do sistema.
     */
    void delete(String id);

    Optional<Client> findByUserAndProvider(String userId, String serviceProviderId);
}