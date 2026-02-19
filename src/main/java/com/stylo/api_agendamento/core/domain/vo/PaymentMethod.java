package com.stylo.api_agendamento.core.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    PIX("Pix", true),
    CREDIT_CARD("Cartão de Crédito", true),
    DEBIT_CARD("Cartão de Débito", false),
    CASH("Dinheiro", false);

    private final String description;
    private final boolean isOnline; // Renomeado de isDigital para clareza (Online via App vs Maquininha física)
}