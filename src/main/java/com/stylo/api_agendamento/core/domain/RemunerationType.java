package com.stylo.api_agendamento.core.domain;

public enum RemunerationType {
    COMMISSION,    // Porcentagem sobre o valor do serviço
    FIXED_FEE,     // Valor fixo para o profissional por serviço
    CHAIR_RENTAL   // O profissional leva 100% (o salão ganha no aluguel fixo mensal)
}