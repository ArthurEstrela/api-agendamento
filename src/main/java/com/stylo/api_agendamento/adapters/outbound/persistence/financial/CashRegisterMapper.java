package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.core.domain.financial.CashRegister;
import com.stylo.api_agendamento.core.domain.financial.CashTransaction;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CashRegisterMapper {

    // ==== De Entidade para Domínio ====
    CashRegister toDomain(CashRegisterEntity entity);

    @Mapping(target = "cashRegisterId", source = "cashRegister.id")
    CashTransaction toDomainTransaction(CashTransactionEntity entity);

    // ==== De Domínio para Entidade ====
    CashRegisterEntity toEntity(CashRegister domain);

    @Mapping(target = "cashRegister", ignore = true) // Ignora no mapeamento direto, resolvemos no AfterMapping
    CashTransactionEntity toEntityTransaction(CashTransaction domain);

    // ✨ A Mágica da Relação Bidirecional:
    // O MapStruct chama este método automaticamente após preencher o CashRegisterEntity
    @AfterMapping
    default void linkTransactionsToRegister(@MappingTarget CashRegisterEntity cashRegister) {
        if (cashRegister.getTransactions() != null) {
            cashRegister.getTransactions().forEach(transaction -> 
                transaction.setCashRegister(cashRegister) // Garante que a Foreign Key não fique nula
            );
        }
    }
}