package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import com.stylo.api_agendamento.core.usecases.dto.FinancialDashboard;
import com.stylo.api_agendamento.core.usecases.dto.DailyCashFlow;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetFinancialDashboardUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IFinancialRepository financialRepository;

    public FinancialDashboard execute(String providerId, LocalDate startDate, LocalDate endDate) {
        // 1. Busca agendamentos concluídos no período
        List<Appointment> revenues = appointmentRepository
                .findAllByProviderIdAndPeriod(providerId, startDate.atStartOfDay(), endDate.atTime(23, 59))
                .stream()
                .filter(app -> app.getStatus() == AppointmentStatus.COMPLETED)
                .filter(app -> !app.isPersonalBlock())
                .toList();

        // 2. Busca despesas no período
        List<Expense> expenses = financialRepository.findAllExpensesByProviderIdAndPeriod(providerId, startDate, endDate);

        // 3. Cálculos Totais
        BigDecimal totalRevenue = revenues.stream()
                .map(Appointment::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. Mapeamento para o fluxo diário (Gráfico do Dashboard)
        Map<LocalDate, BigDecimal> revenueMap = revenues.stream()
                .collect(Collectors.groupingBy(a -> a.getStartTime().toLocalDate(), 
                        Collectors.reducing(BigDecimal.ZERO, Appointment::getFinalPrice, BigDecimal::add)));

        Map<LocalDate, BigDecimal> expenseMap = expenses.stream()
        .collect(Collectors.groupingBy(
                expense -> expense.getDate().toLocalDate(),
                Collectors.reducing(
                        BigDecimal.ZERO, 
                        Expense::getAmount, 
                        BigDecimal::add
                )
        ));

        // Gera a lista de DailyCashFlow para cada dia do período
        List<DailyCashFlow> dailyFlow = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> new DailyCashFlow(
                        date,
                        revenueMap.getOrDefault(date, BigDecimal.ZERO),
                        expenseMap.getOrDefault(date, BigDecimal.ZERO)
                )).toList();

        return new FinancialDashboard(
                totalRevenue,
                totalExpenses,
                totalRevenue.subtract(totalExpenses), // Lucro Líquido
                dailyFlow
        );
    }
}