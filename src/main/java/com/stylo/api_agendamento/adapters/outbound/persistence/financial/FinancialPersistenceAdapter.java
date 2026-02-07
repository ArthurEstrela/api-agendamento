package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.adapters.outbound.persistence.appointment.JpaAppointmentRepository;
import com.stylo.api_agendamento.adapters.outbound.persistence.expense.JpaExpenseRepository;
import com.stylo.api_agendamento.adapters.outbound.persistence.expense.ExpenseMapper;
import com.stylo.api_agendamento.adapters.outbound.persistence.appointment.AppointmentMapper;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FinancialPersistenceAdapter implements IFinancialRepository {

    private final JpaExpenseRepository jpaExpenseRepository;
    private final JpaAppointmentRepository jpaAppointmentRepository;
    private final ExpenseMapper expenseMapper;
    private final AppointmentMapper appointmentMapper;

    @Override
    public Expense saveExpense(Expense expense) {
        var entity = expenseMapper.toEntity(expense);
        var savedEntity = jpaExpenseRepository.save(entity);
        return expenseMapper.toDomain(savedEntity);
    }

    @Override
    public List<Expense> findAllExpensesByProviderInPeriod(String providerId, LocalDateTime start, LocalDateTime end) {
        return jpaExpenseRepository.findAllByProviderIdAndDateBetween(
                UUID.fromString(providerId), start, end)
                .stream()
                .map(expenseMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findCompletedAppointmentsInPeriod(String providerId, LocalDateTime start, LocalDateTime end) {
        // Busca agendamentos com status COMPLETED para calcular a receita
        // Nota: Certifique-se que o JpaAppointmentRepository tem este método
        return jpaAppointmentRepository.findAllByProviderIdAndStatusAndStartTimeBetween(
                UUID.fromString(providerId), "COMPLETED", start, end)
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteExpense(String expenseId) {
        jpaExpenseRepository.deleteById(UUID.fromString(expenseId));
    }
}