package com.stylo.api_agendamento.core.ports;

import java.time.LocalDateTime;
import java.util.Optional;

import com.stylo.api_agendamento.core.domain.Appointment;

public interface IAppointmentRepository {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(String id);
    boolean hasConflictingAppointment(String professionalId, LocalDateTime start, LocalDateTime end);
}