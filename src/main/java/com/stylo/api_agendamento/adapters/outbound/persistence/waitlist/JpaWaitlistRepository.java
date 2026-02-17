package com.stylo.api_agendamento.adapters.outbound.persistence.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface JpaWaitlistRepository extends JpaRepository<WaitlistEntity, UUID> {

    @Query("SELECT w FROM WaitlistEntity w WHERE w.professionalId = :profId AND w.desiredDate = :date AND w.notified = false ORDER BY w.requestTime ASC")
    List<WaitlistEntity> findActiveByProfessionalAndDate(@Param("profId") UUID professionalId, @Param("date") LocalDate date);
}