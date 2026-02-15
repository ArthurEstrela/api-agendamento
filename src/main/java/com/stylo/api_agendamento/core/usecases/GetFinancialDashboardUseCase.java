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
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class GetFinancialDashboardUseCase {

        private final IAppointmentRepository appointmentRepository;
        private final IFinancialRepository financialRepository;

        public FinancialDashboard execute(String providerId, LocalDate startDate, LocalDate endDate) {
                // Validação de segurança para o período
                if (startDate.isAfter(endDate)) {
                        throw new BusinessException("A data inicial não pode ser maior que a data final.");
                }

                // 1. Busca apenas Receitas Reais (Filtro feito diretamente no Banco para
                // performance)
                List<Appointment> revenues = appointmentRepository
                                .findRevenueInPeriod(providerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

                // 2. Busca Despesas
                List<Expense> expenses = financialRepository
                                .findAllExpensesByProviderIdAndPeriod(providerId, startDate, endDate);

                // 3. Agrupamento por dia (Otimizado com Collectors)
                Map<LocalDate, BigDecimal> dailyRevenues = revenues.stream()
                                .collect(Collectors.groupingBy(
                                                a -> a.getStartTime().toLocalDate(),
                                                Collectors.reducing(BigDecimal.ZERO, Appointment::getFinalPrice,
                                                                BigDecimal::add)));

                Map<LocalDate, BigDecimal> dailyExpenses = expenses.stream()
                                .collect(Collectors.groupingBy(
                                                e -> e.getDate().toLocalDate(),
                                                Collectors.reducing(BigDecimal.ZERO, Expense::getAmount,
                                                                BigDecimal::add)));

                // 4. Consolidação do Fluxo Diário para o Gráfico
                List<DailyCashFlow> dailyFlow = startDate.datesUntil(endDate.plusDays(1))
                                .map(date -> new DailyCashFlow(
                                                date,
                                                dailyRevenues.getOrDefault(date, BigDecimal.ZERO),
                                                dailyExpenses.getOrDefault(date, BigDecimal.ZERO)))
                                .collect(Collectors.toList());

                // 5. Cálculo dos Totais Finais
                BigDecimal totalRevenue = revenues.stream()
                                .map(Appointment::getFinalPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalExpenses = expenses.stream()
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new FinancialDashboard(
                                totalRevenue,
                                totalExpenses,
                                totalRevenue.subtract(totalExpenses), // Lucro Líquido Real
                                dailyFlow);
        }
}