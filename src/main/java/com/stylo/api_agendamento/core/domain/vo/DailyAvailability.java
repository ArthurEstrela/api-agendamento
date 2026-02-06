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
        if (isOpen && startTime != null && endTime != null) {
            if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
                throw new BusinessException("O horário de início deve ser anterior ao horário de término.");
            }
        }
    }

    // Verifica se um horário específico cabe dentro deste dia de trabalho
    public boolean contains(LocalTime start, int durationMinutes) {
        if (!isOpen) return false;
        LocalTime end = start.plusMinutes(durationMinutes);
        return !start.isBefore(startTime) && !end.isAfter(endTime);
    }
}