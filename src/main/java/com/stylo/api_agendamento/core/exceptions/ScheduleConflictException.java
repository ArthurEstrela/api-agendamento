package com.stylo.api_agendamento.core.exceptions;

public class ScheduleConflictException extends BusinessException {
    public ScheduleConflictException(String message) {
        super(message);
    }
}