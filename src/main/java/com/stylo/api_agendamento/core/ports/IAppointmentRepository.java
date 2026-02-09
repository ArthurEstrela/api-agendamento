package com.stylo.api_agendamento.core.ports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.stylo.api_agendamento.core.domain.Appointment;

public interface IAppointmentRepository {
    Appointment save(Appointment appointment);

    Optional<Appointment> findById(String id);

    // Método que faltava para o GetAvailableSlotsUseCase
    List<Appointment> findAllByProfessionalIdAndDate(String professionalId, LocalDate date);

    // Mantemos este para validações atômicas de conflito no momento de salvar
    boolean hasConflictingAppointment(String professionalId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findAllByProviderIdAndPeriod(String providerId, LocalDateTime start, LocalDateTime end);

    List<Appointment> findAppointmentsToNotify(LocalDateTime now);

    List<Appointment> findPendingReminders(LocalDateTime targetTime);

    List<Appointment> findRevenueInPeriod(String providerId, LocalDateTime start, LocalDateTime end);

    BigDecimal sumProfessionalCommissionByPeriod(String professionalId, LocalDateTime start, LocalDateTime end);
}