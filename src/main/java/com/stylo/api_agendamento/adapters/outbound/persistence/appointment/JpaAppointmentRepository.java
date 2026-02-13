package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {

        List<AppointmentEntity> findAllByProfessionalIdAndStartTimeBetween(UUID professionalId, LocalDateTime start,
                        LocalDateTime end);

        List<AppointmentEntity> findAllByProviderIdAndStartTimeBetween(UUID providerId, LocalDateTime start,
                        LocalDateTime end);

        List<AppointmentEntity> findAllByClientId(UUID clientId);

        @Query("SELECT COUNT(a) > 0 FROM AppointmentEntity a " +
                        "WHERE a.professionalId = :profId " +
                        "AND a.status IN ('SCHEDULED', 'PENDING', 'BLOCKED') " +
                        "AND a.startTime < :end AND a.endTime > :start")
        boolean existsOverlapping(@Param("profId") UUID professionalId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT a FROM AppointmentEntity a " +
                        "WHERE a.notified = false " +
                        "AND a.status = 'SCHEDULED' " +
                        "AND a.startTime <= :threshold")
        List<AppointmentEntity> findToNotify(@Param("threshold") LocalDateTime threshold);

        @Query("SELECT a FROM AppointmentEntity a WHERE a.notified = false AND a.status = 'SCHEDULED'")
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
        @Query("""
                            SELECT a FROM AppointmentEntity a
                            WHERE a.providerId = :providerId
                            AND a.status = 'COMPLETED'
                            AND a.isPersonalBlock = false
                            AND a.startTime BETWEEN :start AND :end
                        """)
        List<AppointmentEntity> findRevenueAppointments(
                        @Param("providerId") UUID providerId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // JpaAppointmentRepository.java

        @Query("SELECT SUM(a.serviceProviderFee) FROM AppointmentEntity a " +
                        "WHERE a.providerId = :providerId AND a.status = 'COMPLETED' " +
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
}