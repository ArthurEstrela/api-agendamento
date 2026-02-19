package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record DailyAvailability(
    DayOfWeek dayOfWeek,
    boolean isOpen,
    LocalTime startTime,
    LocalTime endTime
) {
    public DailyAvailability {
        if (isOpen) {
            if (startTime == null || endTime == null) {
                throw new BusinessException("Horários são obrigatórios para dias de funcionamento.");
            }
            if (!endTime.isAfter(startTime)) {
                throw new BusinessException("O horário de término deve ser posterior ao início para " + dayOfWeek);
            }
        }
    }

    public boolean contains(LocalTime start, int durationMinutes) {
        if (!isOpen) return false;
        
        LocalTime end = start.plusMinutes(durationMinutes);
        
        // Verifica se o serviço cabe dentro do expediente
        // (start >= startTime) AND (end <= endTime)
        return !start.isBefore(startTime) && !end.isAfter(endTime);
    }
}