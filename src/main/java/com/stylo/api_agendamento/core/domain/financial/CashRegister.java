package com.stylo.api_agendamento.core.domain.financial;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CashRegister {
    private String id;
    private String providerId;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;
    private BigDecimal initialBalance; // Quanto tinha na gaveta ao abrir
    private BigDecimal finalBalance;   // Quanto o usuário contou ao fechar
    private BigDecimal calculatedBalance; // Quanto o sistema diz que deve ter
    private boolean open; // Status
    private String openedByUserId;
    private String closedByUserId;

    @Builder.Default
    private List<CashTransaction> transactions = new ArrayList<>();

    // --- LÓGICA DE NEGÓCIO ---

    public static CashRegister open(String providerId, String userId, BigDecimal initialBalance) {
        return CashRegister.builder()
                .id(UUID.randomUUID().toString())
                .providerId(providerId)
                .openTime(LocalDateTime.now())
                .initialBalance(initialBalance)
                .calculatedBalance(initialBalance)
                .open(true)
                .openedByUserId(userId)
                .transactions(new ArrayList<>())
                .build();
    }

    public void addTransaction(CashTransactionType type, BigDecimal amount, String description, User user) {
        if (!this.open) {
            throw new BusinessException("O caixa está fechado. Abra o caixa para realizar operações.");
        }

        // Se for Sangria, diminui o saldo. Se for Venda ou Suprimento, aumenta.
        BigDecimal impact = amount;
        if (type == CashTransactionType.BLEED) {
            impact = amount.negate();
        }

        // Validação: Não pode sangrar mais do que tem
        if (type == CashTransactionType.BLEED && this.calculatedBalance.compareTo(amount) < 0) {
            throw new BusinessException("Saldo insuficiente em caixa para realizar esta sangria.");
        }

        this.calculatedBalance = this.calculatedBalance.add(impact);
        
        this.transactions.add(CashTransaction.create(this.id, type, amount, description, user.getId()));
    }

    public void close(String userId, BigDecimal finalCountedBalance) {
        if (!this.open) throw new BusinessException("Caixa já está fechado.");
        
        this.open = false;
        this.closeTime = LocalDateTime.now();
        this.closedByUserId = userId;
        this.finalBalance = finalCountedBalance;
        // A diferença (quebra de caixa) é calculada na exibição: finalBalance - calculatedBalance
    }
}