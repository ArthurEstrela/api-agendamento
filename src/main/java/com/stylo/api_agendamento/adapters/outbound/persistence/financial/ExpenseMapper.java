package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.core.domain.Expense;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    Expense toDomain(ExpenseEntity entity);

    ExpenseEntity toEntity(Expense domain);
}