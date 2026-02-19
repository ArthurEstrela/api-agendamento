package com.stylo.api_agendamento.core.domain.financial;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CashRegister {

    private UUID id;
    private UUID providerId;

    // --- TEMPO ---
    private LocalDateTime openTime;
    private LocalDateTime closeTime;

    // --- SALDOS ---
    private BigDecimal initialBalance;    // Quanto tinha na gaveta ao abrir
    
    private BigDecimal calculatedBalance; // O que o SISTEMA diz que tem (Saldo Teórico)
    
    private BigDecimal finalBalance;      // O que o USUÁRIO contou ao fechar (Saldo Real)
    
    private BigDecimal closingDifference; // Diferença (Quebra de caixa): Real - Teórico

    // --- STATUS ---
    private boolean open; // true = ABERTO, false = FECHADO

    // --- AUDITORIA ---
    private UUID openedByUserId;
    private UUID closedByUserId;

    @Builder.Default
    private List<CashTransaction> transactions = new ArrayList<>();

    // --- FACTORY (ABERTURA DE CAIXA) ---

    public static CashRegister open(UUID providerId, UUID userId, BigDecimal initialBalance) {
        if (providerId == null) throw new BusinessException("ID do estabelecimento obrigatório.");
        if (userId == null) throw new BusinessException("Usuário de abertura obrigatório.");
        
        BigDecimal startBalance = (initialBalance != null) ? initialBalance : BigDecimal.ZERO;

        if (startBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O saldo inicial não pode ser negativo.");
        }

        UUID registerId = UUID.randomUUID();

        CashRegister register = CashRegister.builder()
                .id(registerId)
                .providerId(providerId)
                .openTime(LocalDateTime.now())
                .initialBalance(startBalance)
                .calculatedBalance(startBalance) // Começa igual ao inicial
                .open(true)
                .openedByUserId(userId)
                .transactions(new ArrayList<>())
                .build();

        // Registra a transação inicial de abertura para histórico
        if (startBalance.compareTo(BigDecimal.ZERO) > 0) {
            register.transactions.add(
                CashTransaction.create(registerId, CashTransactionType.OPENING, startBalance, "Saldo Inicial", userId)
            );
        }

        return register;
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void addTransaction(CashTransactionType type, BigDecimal amount, String description, UUID userId) {
        if (!this.open) {
            throw new BusinessException("O caixa está fechado. Abra o caixa para realizar operações.");
        }

        // Validação: Não pode sangrar mais do que tem (evitar caixa negativo)
        if (type.isSubtract() && this.calculatedBalance.compareTo(amount) < 0) {
            throw new BusinessException("Saldo insuficiente em caixa para realizar esta operação (Saldo atual: " + this.calculatedBalance + ").");
        }

        // 1. Cria a transação (validações de valor positivo ocorrem lá)
        CashTransaction transaction = CashTransaction.create(this.id, type, amount, description, userId);
        
        // 2. Atualiza o saldo calculado usando a estratégia do Enum
        this.calculatedBalance = type.apply(this.calculatedBalance, amount);
        
        // 3. Adiciona ao histórico
        this.transactions.add(transaction);
    }

    public void close(UUID userId, BigDecimal finalCountedBalance) {
        if (!this.open) throw new BusinessException("Caixa já está fechado.");
        if (finalCountedBalance == null || finalCountedBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O valor final conferido não pode ser negativo.");
        }

        this.open = false;
        this.closeTime = LocalDateTime.now();
        this.closedByUserId = userId;
        
        this.finalBalance = finalCountedBalance; // O que tinha na gaveta
        
        // Calcula a quebra: (O que tem) - (O que deveria ter)
        // Se negativo = Faltou dinheiro (Prejuízo)
        // Se positivo = Sobrou dinheiro
        this.closingDifference = this.finalBalance.subtract(this.calculatedBalance);
        
        // Registra transação de fechamento (simbólica, valor 0 ou o valor contado, depende da regra. Aqui apenas logamos)
        this.transactions.add(
             CashTransaction.create(this.id, CashTransactionType.CLOSING, BigDecimal.ZERO, "Fechamento de Caixa", userId)
        );
    }

    public List<CashTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    // --- IDENTIDADE ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CashRegister that = (CashRegister) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}