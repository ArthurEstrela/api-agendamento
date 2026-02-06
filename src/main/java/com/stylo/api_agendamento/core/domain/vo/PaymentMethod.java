package com.stylo.api_agendamento.core.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    PIX(true),
    CREDIT_CARD(true),
    CASH(false);

    private final boolean isDigital;
}