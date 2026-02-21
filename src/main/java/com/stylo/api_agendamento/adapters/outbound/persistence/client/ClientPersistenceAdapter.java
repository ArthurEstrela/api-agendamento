package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.ports.IClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
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
    public Optional<Client> findById(UUID id) {
        return jpaClientRepository.findById(id)
                .map(clientMapper::toDomain);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return jpaClientRepository.findByEmail(email)
                .map(clientMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaClientRepository.existsByEmail(email);
    }

    @Override
    public void delete(UUID id) {
        jpaClientRepository.deleteById(id);
    }

    @Override
    public Optional<Client> findByUserAndProvider(UUID userId, UUID serviceProviderId) {
        // ✨ CORREÇÃO MARKETPLACE: Como o cliente é global, buscamos apenas pelo ID
        // (que geralmente é o mesmo do User no seu sistema).
        // Se houver necessidade de filtro por provider, isso será feito via
        // Appointments.
        return jpaClientRepository.findById(userId)
                .map(clientMapper::toDomain);
    }

    @Override
    public PagedResult<Client> findAllByProviderId(UUID providerId, String nameFilter, int page, int size) {
        // Ordena por nome em ordem alfabética
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        // ✨ Chama a query de Marketplace: Filtra globalmente, mas restrito ao histórico
        // do Salão
        Page<ClientEntity> entityPage = jpaClientRepository.findClientsByProviderInteraction(
                providerId,
                nameFilter,
                pageable);

        List<Client> domainItems = entityPage.getContent().stream()
                .map(clientMapper::toDomain)
                .toList();

        return new PagedResult<>(
                domainItems,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages());
    }
}