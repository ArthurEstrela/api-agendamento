package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Expense;
import com.stylo.api_agendamento.core.domain.Payout;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IFinancialRepository {

    // --- DESPESAS ---
    Expense saveExpense(Expense expense);

    List<Expense> findExpensesByProviderAndPeriod(UUID providerId, LocalDateTime start, LocalDateTime end);
    
    PagedResult<Expense> findAllExpensesByProviderId(UUID providerId, int page, int size);

    void deleteExpense(UUID expenseId);

    // --- RECEITAS (CAIXA / VENDAS) ---
    void registerRevenue(UUID serviceProviderId, BigDecimal amount, String description, PaymentMethod paymentMethod);

    /**
     * Calcula o lucro líquido (Taxas de serviço - Despesas) no período.
     */
    BigDecimal findNetProfitByProviderAndPeriod(UUID providerId, LocalDateTime start, LocalDateTime end);

    // --- PAGAMENTOS (COMISSÕES / SAQUES) ---
    Payout savePayout(Payout payout);
    
    List<Payout> findPayoutsByProfessional(UUID professionalId, int page, int size);
}