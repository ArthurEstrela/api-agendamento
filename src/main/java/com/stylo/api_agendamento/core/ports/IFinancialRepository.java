package com.stylo.api_agendamento.core.ports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;

public interface IFinancialRepository {
    void saveExpense(Expense expense);

    List<Expense> findExpensesByProvider(String providerId, LocalDateTime start, LocalDateTime end);

    // Aqui o UseCase vai buscar os Appointments pagos para calcular o lucro
    void registerRevenue(String serviceProviderId, BigDecimal amount, String description, PaymentMethod paymentMethod);
        
    List<Expense> findAllExpensesByProviderIdAndPeriod(String providerId, LocalDate start, LocalDate end);

    void deleteExpense(String expenseId);
}