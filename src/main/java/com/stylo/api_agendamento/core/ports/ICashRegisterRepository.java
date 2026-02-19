package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.financial.CashRegister;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICashRegisterRepository {
    
    CashRegister save(CashRegister cashRegister);

    /**
     * Busca o caixa atualmente ABERTO para o estabelecimento.
     * Regra: Só pode haver 1 caixa aberto por Provider.
     */
    Optional<CashRegister> findOpenByProviderId(UUID providerId);

    Optional<CashRegister> findById(UUID id);

    /**
     * Histórico de caixas fechados por período.
     */
    List<CashRegister> findClosedByProviderAndPeriod(UUID providerId, LocalDateTime start, LocalDateTime end);
}