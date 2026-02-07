package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, UUID> {
    // Exemplo de busca para validar conflitos que seu Use Case vai precisar
    List<AppointmentEntity> findAllByProfessionalIdAndStartTimeBetween(UUID professionalId, LocalDateTime start, LocalDateTime end);
}