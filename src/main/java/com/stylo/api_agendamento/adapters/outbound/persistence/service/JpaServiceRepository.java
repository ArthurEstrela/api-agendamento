package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaServiceRepository extends JpaRepository<ServiceEntity, UUID> {
    
    // Busca todos os serviços por estabelecimento
    List<ServiceEntity> findAllByServiceProviderId(UUID serviceProviderId);
    
    // Filtro para serviços ativos (Exibição para o Cliente final)
    List<ServiceEntity> findAllByServiceProviderIdAndIsActiveTrue(UUID serviceProviderId);
    
    // Busca serviços específicos por ID dentro de um estabelecimento (Segurança extra)
    List<ServiceEntity> findAllByIdInAndServiceProviderId(List<UUID> ids, UUID serviceProviderId);

    // Busca por categoria
    List<ServiceEntity> findAllByCategoryId(UUID categoryId);
}