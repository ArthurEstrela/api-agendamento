package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import com.stylo.api_agendamento.core.domain.financial.CashTransactionType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.ICashRegisterRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ManageCashRegisterUseCase {

    private final ICashRegisterRepository repository;
    private final IUserContext userContext;

    /**
     * Abre um novo caixa para o dia de trabalho.
     */
    @Transactional
    public CashRegister openRegister(BigDecimal initialBalance) {
        User user = userContext.getCurrentUser();
        UUID providerId = user.getProviderId();

        // 1. Regra de Negócio: Apenas um caixa aberto por vez por estabelecimento
        repository.findOpenByProviderId(providerId).ifPresent(c -> {
            throw new BusinessException("Já existe um caixa aberto para este estabelecimento.");
        });

        // 2. Factory de Domínio (Gera o evento de abertura automaticamente)
        CashRegister newRegister = CashRegister.open(providerId, user.getId(), initialBalance);
        
        log.info("Caixa aberto por {} no estabelecimento {} com saldo inicial de R$ {}", 
                user.getName(), providerId, initialBalance);
                
        return repository.save(newRegister);
    }

    /**
     * Realiza uma operação (Sangria ou Suprimento) no caixa aberto.
     */
    @Transactional
    public CashRegister addOperation(CashTransactionType type, BigDecimal amount, String description) {
        User user = userContext.getCurrentUser();
        
        CashRegister register = repository.findOpenByProviderId(user.getProviderId())
                .orElseThrow(() -> new BusinessException("Nenhum caixa aberto encontrado para realizar a operação."));

        // O domínio valida se há saldo suficiente para sangria (BLEED)
        register.addTransaction(type, amount, description, user.getId());
        
        return repository.save(register);
    }

    /**
     * Fecha o caixa e calcula a quebra (diferença entre esperado e contado).
     */
    @Transactional
    public CashRegister closeRegister(BigDecimal finalCountedBalance) {
        User user = userContext.getCurrentUser();

        CashRegister register = repository.findOpenByProviderId(user.getProviderId())
                .orElseThrow(() -> new BusinessException("Não há caixa aberto para realizar o fechamento."));

        // Domínio calcula a diferença entre calculatedBalance e finalCountedBalance
        register.close(user.getId(), finalCountedBalance);
        
        log.info("Caixa fechado por {}. Diferença apurada: R$ {}", user.getName(), register.getClosingDifference());
        
        return repository.save(register);
    }

    /**
     * Consulta rápida do estado atual do caixa para o dashboard.
     */
    public Optional<CashRegister> getCurrentStatus(UUID providerId) {
        return repository.findOpenByProviderId(providerId);
    }
}