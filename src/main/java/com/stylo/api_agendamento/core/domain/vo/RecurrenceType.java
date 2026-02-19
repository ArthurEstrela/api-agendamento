package com.stylo.api_agendamento.core.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecurrenceType {
    DAILY("Diário"),
    WEEKLY("Semanal"),
    BIWEEKLY("Quinzenal"),
    MONTHLY("Mensal");
    
    private final String description;
}