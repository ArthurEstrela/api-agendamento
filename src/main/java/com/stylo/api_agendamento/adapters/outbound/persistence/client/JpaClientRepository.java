package com.stylo.api_agendamento.adapters.outbound.persistence.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientRepository extends JpaRepository<ClientEntity, UUID> {

    Optional<ClientEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    // ✨ CORREÇÃO MARKETPLACE: O cliente é global, então buscamos apenas pelo ID dele (vindo do token/User)
    // Se o cliente for o mesmo que o usuário logado, o ID do ClientEntity deve ser o mesmo do User ou ter um vínculo.
    Optional<ClientEntity> findById(UUID id);

    // ✨ BUSCA OTIMIZADA: Agora busca clientes de forma global por nome.
    // Se você quiser listar apenas clientes que já agendaram com um prestador X, 
    // essa lógica deve ser feita via JOIN na tabela de Appointments no futuro.
    @Query("""
            SELECT c FROM ClientEntity c
            WHERE (:nameFilter IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :nameFilter, '%')))
           """)
    Page<ClientEntity> findAllByNameFilter(
            @Param("nameFilter") String nameFilter,
            Pageable pageable);
}