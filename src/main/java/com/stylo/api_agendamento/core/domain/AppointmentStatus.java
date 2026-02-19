package com.stylo.api_agendamento.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppointmentStatus {
    
    PENDING("Pendente"),        // Aguardando aprovação ou pagamento
    SCHEDULED("Agendado"),      // Confirmado pelo sistema
    CONFIRMED("Confirmado"),    // Confirmado manualmente pelo profissional ou pagamento aprovado
    
    COMPLETED("Concluído"),     // Serviço realizado
    
    CANCELLED("Cancelado"),     // Cancelado pelo cliente ou profissional
    NO_SHOW("Não Compareceu"),  // Cliente faltou sem avisar
    BLOCKED("Bloqueado");       // Bloqueio de agenda do profissional

    private final String description;

    public boolean isTerminalState() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }

    public boolean canBeCancelled() {
        return this == PENDING || this == SCHEDULED || this == CONFIRMED;
    }

    public boolean canBeRescheduled() {
        return this == PENDING || this == SCHEDULED || this == CONFIRMED;
    }
    
    public boolean isActive() {
        return !isTerminalState() && this != BLOCKED;
    }
}