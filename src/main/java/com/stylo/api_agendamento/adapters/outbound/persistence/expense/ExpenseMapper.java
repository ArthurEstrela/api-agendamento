package com.stylo.api_agendamento.adapters.outbound.persistence.expense;

import com.stylo.api_agendamento.core.domain.Expense;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {
    ExpenseEntity toEntity(Expense domain);
    Expense toDomain(ExpenseEntity entity);
}