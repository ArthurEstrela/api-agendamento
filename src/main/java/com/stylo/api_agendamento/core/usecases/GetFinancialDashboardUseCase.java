package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GetFinancialDashboardUseCase {
    private final IAppointmentRepository appointmentRepo;
    private final IFinancialRepository financialRepo;

    public FinancialData execute(String providerId, LocalDateTime start, LocalDateTime end) {
        // 1. Busca todos os agendamentos "COMPLETED" no período
        // 2. Soma o totalRevenue
        // 3. Busca as despesas (Expenses)
        // 4. Calcula o netIncome (Receita - Despesas)
        // 5. Retorna um DTO de Dashboard
        return null; // Implementação da lógica de cálculo
    }
}