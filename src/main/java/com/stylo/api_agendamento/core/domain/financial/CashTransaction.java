package com.stylo.api_agendamento.core.domain.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CashTransaction {
    private String id;
    private String cashRegisterId;
    private CashTransactionType type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;
    private String performedByUserId;

    public static CashTransaction create(String registerId, CashTransactionType type, BigDecimal amount, String desc, String userId) {
        return CashTransaction.builder()
                .id(UUID.randomUUID().toString())
                .cashRegisterId(registerId)
                .type(type)
                .amount(amount)
                .description(desc)
                .timestamp(LocalDateTime.now())
                .performedByUserId(userId)
                .build();
    }
}