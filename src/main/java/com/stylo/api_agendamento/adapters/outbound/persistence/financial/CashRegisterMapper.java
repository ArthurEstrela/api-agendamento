package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import com.stylo.api_agendamento.core.domain.financial.CashTransaction;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CashRegisterMapper {

    public CashRegister toDomain(CashRegisterEntity entity) {
        if (entity == null) return null;

        List<CashTransaction> transactions = entity.getTransactions() == null ? 
                Collections.emptyList() :
                entity.getTransactions().stream()
                        .map(this::toDomainTransaction)
                        .collect(Collectors.toList());

        return CashRegister.builder()
                .id(entity.getId())
                .providerId(entity.getProviderId())
                .openTime(entity.getOpenTime())
                .closeTime(entity.getCloseTime())
                .initialBalance(entity.getInitialBalance())
                .finalBalance(entity.getFinalBalance())
                .calculatedBalance(entity.getCalculatedBalance())
                .open(entity.isOpen())
                .openedByUserId(entity.getOpenedByUserId())
                .closedByUserId(entity.getClosedByUserId())
                .transactions(transactions)
                .build();
    }

    private CashTransaction toDomainTransaction(CashTransactionEntity entity) {
        return CashTransaction.builder()
                .id(entity.getId())
                .cashRegisterId(entity.getCashRegister().getId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .timestamp(entity.getTimestamp())
                .performedByUserId(entity.getPerformedByUserId())
                .build();
    }

    public CashRegisterEntity toEntity(CashRegister domain) {
        if (domain == null) return null;

        CashRegisterEntity entity = CashRegisterEntity.builder()
                .id(domain.getId())
                .providerId(domain.getProviderId())
                .openTime(domain.getOpenTime())
                .closeTime(domain.getCloseTime())
                .initialBalance(domain.getInitialBalance())
                .finalBalance(domain.getFinalBalance())
                .calculatedBalance(domain.getCalculatedBalance())
                .isOpen(domain.isOpen())
                .openedByUserId(domain.getOpenedByUserId())
                .closedByUserId(domain.getClosedByUserId())
                .build();

        // Mapeia transações e configura a relação bidirecional
        if (domain.getTransactions() != null) {
            List<CashTransactionEntity> transactionEntities = domain.getTransactions().stream()
                    .map(t -> {
                        var tEntity = toEntityTransaction(t);
                        tEntity.setCashRegister(entity); // Vincula o pai
                        return tEntity;
                    })
                    .collect(Collectors.toList());
            entity.setTransactions(transactionEntities);
        }

        return entity;
    }

    private CashTransactionEntity toEntityTransaction(CashTransaction domain) {
        return CashTransactionEntity.builder()
                .id(domain.getId())
                .type(domain.getType())
                .amount(domain.getAmount())
                .description(domain.getDescription())
                .timestamp(domain.getTimestamp())
                .performedByUserId(domain.getPerformedByUserId())
                .build();
    }
}