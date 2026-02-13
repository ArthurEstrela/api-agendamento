package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.adapters.outbound.persistence.appointment.JpaAppointmentRepository;
import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialPersistenceAdapter implements IFinancialRepository {

    private final JpaExpenseRepository jpaExpenseRepository;
    private final ExpenseMapper expenseMapper;

    @Override
    public void saveExpense(Expense expense) {
        var entity = expenseMapper.toEntity(expense);
        jpaExpenseRepository.save(entity);
    }

    @Override
    public void deleteExpense(String expenseId) {
        // Implementação do método que estava faltando
        jpaExpenseRepository.deleteById(UUID.fromString(expenseId));
    }

    @Override
    public List<Expense> findExpensesByProvider(String providerId, LocalDateTime start, LocalDateTime end) {
        return jpaExpenseRepository.findAllByServiceProviderIdAndDateBetween(
                UUID.fromString(providerId), start, end)
                .stream()
                .map(expenseMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Expense> findAllExpensesByProviderIdAndPeriod(String providerId, LocalDate start, LocalDate end) {
        // Converte o período do dia para LocalDateTime
        return findExpensesByProvider(providerId, start.atStartOfDay(), end.atTime(23, 59, 59));
    }

    @Override
    public void registerRevenue(String serviceProviderId, BigDecimal amount, String description,
            com.stylo.api_agendamento.core.domain.vo.PaymentMethod paymentMethod) {
        // No fluxo atual, a receita é extraída dos agendamentos com status 'COMPLETED'.
        // Este método pode ser usado futuramente para registrar vendas avulsas que não
        // são agendamentos.
        log.info("Receita registrada via webhook/manual: Provider {} - Valor R$ {}", serviceProviderId, amount);
    }

    // FinancialPersistenceAdapter.java
    private final JpaAppointmentRepository jpaAppointmentRepository;

    public BigDecimal getNetProfit(String providerId, LocalDateTime start, LocalDateTime end) {
        BigDecimal totalFees = jpaAppointmentRepository.sumNetRevenue(UUID.fromString(providerId), start, end);

        // Busca despesas usando o repositório que você já tem
        List<ExpenseEntity> expenses = jpaExpenseRepository.findAllByServiceProviderIdAndDateBetween(
                UUID.fromString(providerId), start, end);

        BigDecimal totalExpenses = expenses.stream()
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return (totalFees != null ? totalFees : BigDecimal.ZERO).subtract(totalExpenses);
    }
}