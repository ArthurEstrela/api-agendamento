package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AppointmentPersistenceAdapter implements IAppointmentRepository {

    private final JpaAppointmentRepository jpaAppointmentRepository;
    private final AppointmentMapper appointmentMapper;

    @Override
    public Appointment save(Appointment appointment) {
        var entity = appointmentMapper.toEntity(appointment);
        var savedEntity = jpaAppointmentRepository.save(entity);
        return appointmentMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return jpaAppointmentRepository.findById(UUID.fromString(id))
                .map(appointmentMapper::toDomain);
    }

    @Override
    public List<Appointment> findAllByProfessionalIdAndDate(String professionalId, LocalDate date) {
        var startOfDay = date.atStartOfDay();
        var endOfDay = date.atTime(23, 59, 59);

        return jpaAppointmentRepository.findAllByProfessionalIdAndStartTimeBetween(
                UUID.fromString(professionalId), startOfDay, endOfDay)
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findAllByProviderIdAndPeriod(String providerId, LocalDateTime start, LocalDateTime end) {
        return jpaAppointmentRepository.findAllByProviderIdAndStartTimeBetween(
                UUID.fromString(providerId), start, end)
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasConflictingAppointment(String professionalId, LocalDateTime start, LocalDateTime end) {
        return jpaAppointmentRepository.existsOverlapping(UUID.fromString(professionalId), start, end);
    }

    // Implementação corrigida para satisfazer a interface
    @Override
    public List<Appointment> findPendingReminders(LocalDateTime now) {
        // Chamamos o JpaRepository passando o tempo atual para o cálculo da query
        return jpaAppointmentRepository.findPendingReminders(now)
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    // Método antigo/duplicado que pode ser removido se não estiver na interface
    @Override
    public List<Appointment> findAppointmentsToNotify(LocalDateTime threshold) {
        return jpaAppointmentRepository.findToNotify(threshold)
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Appointment> findRevenueInPeriod(String providerId, LocalDateTime start, LocalDateTime end) {
        return jpaAppointmentRepository.findRevenueAppointments(UUID.fromString(providerId), start, end)
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumProfessionalCommissionByPeriod(String professionalId, LocalDateTime start, LocalDateTime end) {
        BigDecimal result = jpaAppointmentRepository.sumProfessionalCommissionByPeriod(
                UUID.fromString(professionalId), start, end);

        // Garantir que não retorne null caso não existam agendamentos no período
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public List<Appointment> findAllByClientId(String clientId) {
        // ✨ Converte a String ID para UUID e mapeia para o Domínio
        return jpaAppointmentRepository.findAllByClientId(UUID.fromString(clientId))
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }
}
