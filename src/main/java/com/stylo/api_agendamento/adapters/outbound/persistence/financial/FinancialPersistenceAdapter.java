package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.adapters.outbound.persistence.appointment.JpaAppointmentRepository;
import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.domain.Payout;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialPersistenceAdapter implements IFinancialRepository {

    private final JpaExpenseRepository jpaExpenseRepository;
    private final ExpenseMapper expenseMapper;
    private final JpaPayoutRepository jpaPayoutRepository;
    private final PayoutMapper payoutMapper;
    private final JpaAppointmentRepository jpaAppointmentRepository;

    @Override
    public Expense saveExpense(Expense expense) {
        var entity = expenseMapper.toEntity(expense);
        var savedEntity = jpaExpenseRepository.save(entity);
        return expenseMapper.toDomain(savedEntity);
    }

    @Override
    public void deleteExpense(UUID expenseId) {
        jpaExpenseRepository.deleteById(expenseId);
    }

    @Override
    public List<Expense> findExpensesByProviderAndPeriod(UUID providerId, LocalDateTime start, LocalDateTime end) {
        return jpaExpenseRepository.findAllByServiceProviderIdAndDateBetween(providerId, start, end)
                .stream()
                .map(expenseMapper::toDomain)
                .toList();
    }

    @Override
    public PagedResult<Expense> findAllExpensesByProviderId(UUID providerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        Page<ExpenseEntity> entityPage = jpaExpenseRepository.findAllByServiceProviderId(providerId, pageable);

        List<Expense> items = entityPage.getContent().stream()
                .map(expenseMapper::toDomain)
                .toList();

        return new PagedResult<>(
                items,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }

    // ✨ CORREÇÃO: A interface espera List<Payout> e não PagedResult
    @Override
    public List<Payout> findPayoutsByProfessional(UUID professionalId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("processedAt").descending());
        return jpaPayoutRepository.findAllByProfessionalId(professionalId, pageable)
                .stream()
                .map(payoutMapper::toDomain)
                .toList();
    }

    @Override
    public void registerRevenue(UUID serviceProviderId, BigDecimal amount, String description, PaymentMethod paymentMethod) {
        log.info("Receita registrada: Provider {} - Valor R$ {}", serviceProviderId, amount);
    }

    // ✨ CORREÇÃO: Nome do método alterado para coincidir com a interface
    @Override
    public BigDecimal findNetProfitByProviderAndPeriod(UUID providerId, LocalDateTime start, LocalDateTime end) {
        BigDecimal totalFees = jpaAppointmentRepository.sumNetRevenue(providerId, start, end);
        List<ExpenseEntity> expenses = jpaExpenseRepository.findAllByServiceProviderIdAndDateBetween(providerId, start, end);

        BigDecimal totalExpenses = expenses.stream()
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return (totalFees != null ? totalFees : BigDecimal.ZERO).subtract(totalExpenses);
    }

    @Override
    public Payout savePayout(Payout payout) {
        var entity = payoutMapper.toEntity(payout);
        var savedEntity = jpaPayoutRepository.save(entity);
        return payoutMapper.toDomain(savedEntity);
    }
}