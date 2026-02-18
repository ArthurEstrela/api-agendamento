package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import com.stylo.api_agendamento.core.ports.ICashRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CashRegisterPersistenceAdapter implements ICashRegisterRepository {

    private final JpaCashRegisterRepository repository;
    private final CashRegisterMapper mapper;

    @Override
    public CashRegister save(CashRegister cashRegister) {
        var entity = mapper.toEntity(cashRegister);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CashRegister> findOpenByProviderId(String providerId) {
        return repository.findOpenByProviderId(providerId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CashRegister> findById(String id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }
}