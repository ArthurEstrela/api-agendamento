package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import com.stylo.api_agendamento.core.domain.financial.CashTransactionType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.ICashRegisterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManageCashRegisterUseCase {

    private final ICashRegisterRepository repository;

    @Transactional
    public CashRegister openRegister(User user, BigDecimal initialBalance) {
        // Verifica se já existe caixa aberto para este provider
        Optional<CashRegister> existing = repository.findOpenByProviderId(user.getProviderId());
        if (existing.isPresent()) {
            throw new BusinessException("Já existe um caixa aberto para este estabelecimento.");
        }

        CashRegister newRegister = CashRegister.open(user.getProviderId(), user.getId(), initialBalance);
        return repository.save(newRegister);
    }

    @Transactional
    public CashRegister addOperation(User user, CashTransactionType type, BigDecimal amount, String description) {
        CashRegister register = repository.findOpenByProviderId(user.getProviderId())
                .orElseThrow(() -> new BusinessException("Não há caixa aberto no momento."));

        register.addTransaction(type, amount, description, user);
        return repository.save(register);
    }

    @Transactional
    public CashRegister closeRegister(User user, BigDecimal finalBalance) {
        CashRegister register = repository.findOpenByProviderId(user.getProviderId())
                .orElseThrow(() -> new BusinessException("Não há caixa aberto para fechar."));

        register.close(user.getId(), finalBalance);
        return repository.save(register);
    }
    
    public Optional<CashRegister> getCurrentStatus(String providerId) {
        return repository.findOpenByProviderId(providerId);
    }
}