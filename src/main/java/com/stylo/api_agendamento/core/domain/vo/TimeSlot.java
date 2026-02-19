package com.stylo.api_agendamento.core.domain.vo;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import java.time.LocalTime;

public record TimeSlot(LocalTime start, LocalTime end) {
    public TimeSlot {
        if (start == null || end == null) {
            throw new BusinessException("Horários de início e fim são obrigatórios.");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("O horário de término deve ser posterior ao início.");
        }
    }

    /**
     * Verifica se existe interseção entre dois intervalos de tempo.
     * Intervalos que apenas se "tocam" (ex: 10:00-11:00 e 11:00-12:00) NÃO sobrepõem.
     */
    public boolean overlaps(TimeSlot other) {
        return this.start.isBefore(other.end) && this.end.isAfter(other.start);
    }
    
    public int getDurationMinutes() {
        return (int) java.time.Duration.between(start, end).toMinutes();
    }
}