// src/main/java/com/stylo/api_agendamento/adapters/outbound/persistence/calendar/JpaGoogleTokenRepository.java
package com.stylo.api_agendamento.adapters.outbound.persistence.calendar;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface JpaGoogleTokenRepository extends JpaRepository<GoogleTokenEntity, UUID> {
}