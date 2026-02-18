package com.stylo.api_agendamento.core.domain;

public enum AppointmentStatus {
    PENDING,
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    BLOCKED,
    NO_SHOW;

    public boolean isTerminalState() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }
}