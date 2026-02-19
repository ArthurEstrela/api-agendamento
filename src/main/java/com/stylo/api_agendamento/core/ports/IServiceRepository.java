package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IServiceRepository {
    
    Service save(Service service);

    Optional<Service> findById(UUID id);

    void delete(UUID id); // Soft delete recomendado na implementação

    /**
     * Busca serviços pelos IDs.
     * Validação essencial no CreateAppointmentUseCase.
     */
    List<Service> findAllByIds(List<UUID> ids);

    /**
     * Lista todos os serviços do estabelecimento (Gestão).
     */
    List<Service> findAllByProviderId(UUID providerId);

    /**
     * Lista apenas serviços ativos (para o cliente agendar).
     */
    List<Service> findAllActiveByProviderId(UUID providerId);
}