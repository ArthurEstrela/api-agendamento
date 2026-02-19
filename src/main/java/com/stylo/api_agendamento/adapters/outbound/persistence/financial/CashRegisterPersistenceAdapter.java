package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import com.stylo.api_agendamento.core.ports.ICashRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<CashRegister> findOpenByProviderId(UUID providerId) {
        return repository.findByProviderIdAndIsOpenTrue(providerId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<CashRegister> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    // ✨ NOVA IMPLEMENTAÇÃO
    @Override
    public List<CashRegister> findClosedByProviderAndPeriod(UUID providerId, LocalDateTime start, LocalDateTime end) {
        return repository.findByProviderIdAndIsOpenFalseAndCloseTimeBetween(providerId, start, end)
                .stream()
                .map(mapper::toDomain)
                .toList(); // Usando Java 16+ toList()
    }
}