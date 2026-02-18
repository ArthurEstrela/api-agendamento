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

        // CORREÇÃO: Passamos Pageable.unpaged() para satisfazer a assinatura do Repository
        // sem aplicar limite de registros, já que precisamos de todos para o cálculo do dia.
        return jpaAppointmentRepository.findAllByProfessionalIdAndStartTimeBetween(
                UUID.fromString(professionalId), startOfDay, endOfDay, Pageable.unpaged())
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

    @Override
    public List<Appointment> findPendingReminders(LocalDateTime now) {
        return jpaAppointmentRepository.findPendingReminders(now)
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }

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

        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public PagedResult<Appointment> findAllByClientId(String clientId, int page, int size) {
        // Agora usa corretamente PageRequest e Sort importados
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());

        Page<AppointmentEntity> entityPage = jpaAppointmentRepository.findAllByClientId(
                UUID.fromString(clientId),
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
    public List<Appointment> findPendingSettlementByProfessional(String professionalId) {
        return jpaAppointmentRepository
                .findAllByProfessionalIdAndCommissionSettledFalse(UUID.fromString(professionalId))
                .stream()
                .map(appointmentMapper::toDomain)
                .collect(Collectors.toList());
    }
}