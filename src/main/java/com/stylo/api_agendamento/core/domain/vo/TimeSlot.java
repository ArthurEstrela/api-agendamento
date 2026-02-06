package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import java.time.LocalTime;

public record TimeSlot(LocalTime start, LocalTime end) {
    public TimeSlot {
        if (start == null || end == null) {
            throw new BusinessException("Horários de início e fim são obrigatórios.");
        }
        if (start.isAfter(end) || start.equals(end)) {
            throw new BusinessException("O horário de início deve ser anterior ao fim.");
        }
    }

    public boolean overlaps(TimeSlot other) {
        return !start.isAfter(other.end) && !other.start.isAfter(end);
    }
}