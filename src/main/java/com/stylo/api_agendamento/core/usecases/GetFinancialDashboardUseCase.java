package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import com.stylo.api_agendamento.core.usecases.dto.FinancialDashboard;
import com.stylo.api_agendamento.core.usecases.dto.DailyCashFlow;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class GetFinancialDashboardUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IFinancialRepository financialRepository;

    public FinancialDashboard execute(UUID providerId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data inicial não pode ser superior à data final.");
        }

        // 1. Busca Receitas (Agendamentos Concluídos)
        List<Appointment> revenues = appointmentRepository.findRevenueInPeriod(
                providerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

        // 2. Busca Despesas (Lançamentos manuais)
        List<Expense> expenses = financialRepository.findExpensesByProviderAndPeriod(
                providerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

        // 3. Agrupamento por Dia (Mapas de Soma)
        Map<LocalDate, BigDecimal> dailyRevMap = revenues.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStartTime().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Appointment::getFinalPrice, BigDecimal::add)));

        Map<LocalDate, BigDecimal> dailyExpMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCreatedAt().toLocalDate(),
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));

        // 4. Consolidação do Fluxo de Caixa (Preenchendo dias vazios para o gráfico)
        List<DailyCashFlow> dailyFlow = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> new DailyCashFlow(
                        date,
                        dailyRevMap.getOrDefault(date, BigDecimal.ZERO),
                        dailyExpMap.getOrDefault(date, BigDecimal.ZERO)))
                .collect(Collectors.toList());

        // 5. Totais Consolidados
        BigDecimal totalRevenue = dailyFlow.stream()
                .map(DailyCashFlow::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = dailyFlow.stream()
                .map(DailyCashFlow::expenses)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new FinancialDashboard(
                totalRevenue,
                totalExpenses,
                totalRevenue.subtract(totalExpenses), // Net Profit
                dailyFlow
        );
    }
}