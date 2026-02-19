package com.stylo.api_agendamento.core.domain.financial;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CashTransaction {

    private UUID id;
    private UUID cashRegisterId;
    
    private CashTransactionType type;
    private BigDecimal amount;
    private String description;
    
    private LocalDateTime timestamp;
    private UUID performedByUserId; // Quem fez a movimentação

    public static CashTransaction create(UUID registerId, CashTransactionType type, BigDecimal amount, String desc, UUID userId) {
        if (registerId == null) throw new BusinessException("ID do caixa é obrigatório.");
        if (type == null) throw new BusinessException("Tipo de transação é obrigatório.");
        
        // Regra: O valor da transação é sempre positivo (magnitude). 
        // Se é sangria, o Enum trata de subtrair do saldo, mas o registro é de "R$ 50,00".
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor da transação deve ser positivo.");
        }

        if (userId == null) throw new BusinessException("Usuário responsável é obrigatório.");

        return CashTransaction.builder()
                .id(UUID.randomUUID())
                .cashRegisterId(registerId)
                .type(type)
                .amount(amount)
                .description(desc != null ? desc : type.getDescription())
                .timestamp(LocalDateTime.now())
                .performedByUserId(userId)
                .build();
    }
    
    // --- IDENTIDADE ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CashTransaction that = (CashTransaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}