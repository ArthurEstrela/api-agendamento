package com.stylo.api_agendamento.core.ports;

import java.util.List;
import java.util.Optional;

import com.stylo.api_agendamento.core.domain.Professional;

public interface IProfessionalRepository {
    Professional save(Professional professional);

    List<Professional> findAllByProviderId(String providerId);

    Optional<Professional> findById(String id);

    Optional<Professional> findByIdWithLock(String id);
}