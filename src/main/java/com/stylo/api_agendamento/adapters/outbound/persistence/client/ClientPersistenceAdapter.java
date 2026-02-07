package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.ports.IClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientPersistenceAdapter implements IClientRepository {

    private final JpaClientRepository jpaClientRepository;
    private final ClientMapper clientMapper;

    @Override
    public Client save(Client client) {
        var entity = clientMapper.toEntity(client);
        var savedEntity = jpaClientRepository.save(entity);
        return clientMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return jpaClientRepository.findByEmail(email)
                .map(clientMapper::toDomain);
    }

    @Override
    public Optional<Client> findById(String id) {
        return jpaClientRepository.findById(UUID.fromString(id))
                .map(clientMapper::toDomain);
    }

    @Override
    public void delete(String id) {
        // Converte a String do domínio para o UUID da persistência
        jpaClientRepository.deleteById(UUID.fromString(id));
    }
}