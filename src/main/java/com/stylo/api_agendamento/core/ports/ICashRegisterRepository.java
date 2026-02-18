package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import java.util.Optional;

public interface ICashRegisterRepository {
    CashRegister save(CashRegister cashRegister);
    Optional<CashRegister> findOpenByProviderId(String providerId);
    Optional<CashRegister> findById(String id);
    // Futuramente: buscar histórico de caixas fechados
}