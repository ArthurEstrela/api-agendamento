package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

    Page<AppointmentEntity> findAllByProfessionalIdAndStartTimeBetween(
            UUID professionalId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable);

    List<AppointmentEntity> findAllByServiceProviderIdAndStartTimeBetween(UUID serviceProviderId, LocalDateTime start,
            LocalDateTime end);

    Page<AppointmentEntity> findAllByClientId(UUID clientId, Pageable pageable);

    @Query("""
                SELECT COUNT(a) > 0 FROM AppointmentEntity a
                WHERE a.professionalId = :professionalId
                AND a.status != 'CANCELLED'
                AND (a.startTime < :endTime AND a.endTime > :startTime)
            """)
    boolean existsOverlapping(
            @Param("professionalId") UUID professionalId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT a FROM AppointmentEntity a " +
            "WHERE a.reminderSent = false " + // ✨ Atualizado aqui
            "AND a.status = 'SCHEDULED' " +
            "AND a.startTime <= :threshold")
    List<AppointmentEntity> findToNotify(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.reminderSent = false AND a.status = 'SCHEDULED'") // ✨ Atualizado
                                                                                                        // aqui
    List<AppointmentEntity> findAllByNotifiedFalseAndStatusScheduled();

    List<AppointmentEntity> findAllByProviderIdAndStatusAndStartTimeBetween(UUID providerId, String status,
            LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM AppointmentEntity a WHERE a.status = 'CONFIRMED' " +
            "AND a.reminderSent = false " +
            "AND a.startTime <= :limit")
    List<AppointmentEntity> findAppointmentsToRemind(@Param("limit") LocalDateTime limit);

    // No JpaAppointmentRepository.java
    @Query(value = """
            SELECT * FROM appointments a
            WHERE a.status = 'SCHEDULED'
            AND a.reminder_sent = false
            AND (a.start_time - (a.reminder_minutes * interval '1 minute')) <= :now
            """, nativeQuery = true)
    List<AppointmentEntity> findPendingReminders(@Param("now") LocalDateTime now);

    // No arquivo JpaAppointmentRepository.java
    @Query("SELECT a FROM AppointmentEntity a " +
            "WHERE a.serviceProviderId = :providerId " + // ✨ Atualizado de a.providerId para a.serviceProviderId
            "AND a.status = 'COMPLETED' " +
            "AND a.isPersonalBlock = false " +
            "AND a.startTime BETWEEN :start AND :end")
    List<AppointmentEntity> findRevenueAppointments(@Param("providerId") UUID providerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    // JpaAppointmentRepository.java

    @Query("SELECT SUM(a.serviceProviderFee) FROM AppointmentEntity a " +
            "WHERE a.serviceProviderId = :providerId AND a.status = 'COMPLETED' " +
            "AND a.startTime BETWEEN :start AND :end")
    BigDecimal sumNetRevenue(@Param("providerId") UUID providerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT SUM(a.professionalCommission) FROM AppointmentEntity a " +
            "WHERE a.professionalId = :profId " +
            "AND a.status = 'COMPLETED' " +
            "AND a.startTime BETWEEN :start AND :end")
    BigDecimal sumProfessionalCommissionByPeriod(
            @Param("profId") UUID professionalId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<AppointmentEntity> findAllByProfessionalIdAndCommissionSettledFalse(UUID professionalId);

    boolean existsByExternalEventId(String externalEventId);
}