package com.stylo.api_agendamento.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GoogleConnectionStatus {
    PENDING("Pendente"),
    CONNECTED("Conectado"),
    DISCONNECTED("Desconectado"),
    EXPIRED("Token Expirado"), // Precisa reconectar
    ERROR("Erro de Conexão");  // Permissão revogada ou erro técnico

    private final String description;

    public boolean isConnected() {
        return this == CONNECTED;
    }
    
    public boolean needsReconnection() {
        return this == EXPIRED || this == ERROR || this == DISCONNECTED;
    }
}