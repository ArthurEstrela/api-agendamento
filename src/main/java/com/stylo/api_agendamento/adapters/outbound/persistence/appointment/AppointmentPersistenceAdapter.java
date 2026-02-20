package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Appointment> findById(UUID id) {
        return jpaAppointmentRepository.findById(id)
                .map(appointmentMapper::toDomain);
    }

    @Override
    public List<Appointment> findAllByProfessionalIdAndDate(UUID professionalId, LocalDate date) {
        var startOfDay = date.atStartOfDay();
        var endOfDay = date.atTime(23, 59, 59);

        return jpaAppointmentRepository.findAllByProfessionalIdAndStartTimeBetween(
                        professionalId, startOfDay, endOfDay, Pageable.unpaged())
                .stream()
                .map(appointmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findAllByProviderIdAndPeriod(UUID providerId, LocalDateTime start, LocalDateTime end) {
        return jpaAppointmentRepository.findAllByServiceProviderIdAndStartTimeBetween(
                        providerId, start, end)
                .stream()
                .map(appointmentMapper::toDomain)
                .toList();
    }

    @Override
    public boolean hasConflictingAppointment(UUID professionalId, LocalDateTime start, LocalDateTime end) {
        return jpaAppointmentRepository.existsOverlapping(professionalId, start, end);
    }

    @Override
    public List<Appointment> findPendingReminders(LocalDateTime now) {
        return jpaAppointmentRepository.findPendingReminders(now)
                .stream()
                .map(appointmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findAppointmentsToNotify(LocalDateTime threshold) {
        return jpaAppointmentRepository.findToNotify(threshold)
                .stream()
                .map(appointmentMapper::toDomain)
                .toList();
    }

    @Override
    public List<Appointment> findRevenueInPeriod(UUID providerId, LocalDateTime start, LocalDateTime end) {
        return jpaAppointmentRepository.findRevenueAppointments(providerId, start, end)
                .stream()
                .map(appointmentMapper::toDomain)
                .toList();
    }

    @Override
    public BigDecimal sumProfessionalCommissionByPeriod(UUID professionalId, LocalDateTime start, LocalDateTime end) {
        BigDecimal result = jpaAppointmentRepository.sumProfessionalCommissionByPeriod(
                professionalId, start, end);

        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public PagedResult<Appointment> findAllByClientId(UUID clientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());

        Page<AppointmentEntity> entityPage = jpaAppointmentRepository.findAllByClientId(
                clientId,
                pageable);

        List<Appointment> domainItems = entityPage.getContent().stream()
                .map(appointmentMapper::toDomain)
                .toList();

        return new PagedResult<>(
                domainItems,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages());
    }

    @Override
    public List<Appointment> findPendingSettlementByProfessional(UUID professionalId) {
        return jpaAppointmentRepository
                .findAllByProfessionalIdAndCommissionSettledFalse(professionalId)
                .stream()
                .map(appointmentMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByExternalEventId(String externalEventId) {
        // Implementação do método que estava faltando na interface
        return jpaAppointmentRepository.existsByExternalEventId(externalEventId);
    }
}