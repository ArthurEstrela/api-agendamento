package com.stylo.api_agendamento.core.ports;

import java.time.LocalDateTime;
import java.util.List;

import com.stylo.api_agendamento.core.domain.Expense;

public interface IFinancialRepository {
    void saveExpense(Expense expense);
    List<Expense> findExpensesByProvider(String providerId, LocalDateTime start, LocalDateTime end);
    // Aqui o UseCase vai buscar os Appointments pagos para calcular o lucro
}