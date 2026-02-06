package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Expense {
    private final String id;
    private final String providerId;
    private String description;
    private BigDecimal amount;
    private LocalDateTime date;
    private String category;
    private String type; // Usar um Enum aqui seria ainda melhor: ONE_TIME, RECURRING
    private String frequency;

    public static Expense create(String providerId, String description, BigDecimal amount, String category, String type) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor da despesa deve ser positivo.");
        }
        return Expense.builder()
                .providerId(providerId)
                .description(description)
                .amount(amount)
                .date(LocalDateTime.now())
                .category(category)
                .type(type)
                .build();
    }

    public boolean isRecurring() {
        return "recurring".equalsIgnoreCase(this.type);
    }
}