package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import com.stylo.api_agendamento.adapters.outbound.persistence.ExpenseEntity;
import com.stylo.api_agendamento.core.domain.Expense;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FinancialMapper {
    ExpenseEntity toEntity(Expense domain);
    Expense toDomain(ExpenseEntity entity);
}