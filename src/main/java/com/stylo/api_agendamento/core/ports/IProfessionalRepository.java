package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Professional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IProfessionalRepository {
    
    Professional save(Professional professional);

    /**
     * Lista todos os profissionais de um estabelecimento (para dropdowns/agendas).
     */
    List<Professional> findAllByProviderId(UUID providerId);

    Optional<Professional> findById(UUID id);

    /**
     * Busca um profissional e aplica um LOCK PESSIMISTA (SELECT ... FOR UPDATE) no banco.
     * Crítico para garantir consistência ao criar agendamentos concorrentes.
     */
    Optional<Professional> findByIdWithLock(UUID id);

    /**
     * Busca profissional pelo email (Login/Convite).
     */
    Optional<Professional> findByEmail(String email);
    
    /**
     * Busca pelo ID da conta conectada do Stripe (para webhooks de pagamento).
     */
    Optional<Professional> findByGatewayAccountId(String accountId);
}