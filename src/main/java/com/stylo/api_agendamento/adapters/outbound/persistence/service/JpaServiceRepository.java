package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaServiceRepository extends JpaRepository<ServiceEntity, UUID> {
    
    // Busca todos os serviços oferecidos por um estabelecimento específico (Multi-tenant)
    List<ServiceEntity> findAllByServiceProviderId(UUID serviceProviderId);
    
    // Busca serviços específicos por ID dentro de um salão (segurança extra)
    List<ServiceEntity> findAllByIdInAndServiceProviderId(List<UUID> ids, UUID serviceProviderId);

    // Caso queiras filtrar serviços ativos/inativos no futuro
    // List<ServiceEntity> findAllByServiceProviderIdAndActiveTrue(UUID serviceProviderId);
}