package com.stylo.api_agendamento.core.exceptions;

public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String message) {
        super(message);
    }
}