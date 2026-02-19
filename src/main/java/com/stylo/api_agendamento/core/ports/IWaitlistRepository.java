package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Waitlist;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IWaitlistRepository {
    
    Waitlist save(Waitlist waitlist);

    Optional<Waitlist> findById(UUID id);

    /**
     * Busca quem está na fila de espera para um profissional em um dia específico.
     * Útil para notificar automaticamente quando alguém cancela.
     */
    List<Waitlist> findAllByProfessionalIdAndDate(UUID professionalId, LocalDate date);

    /**
     * Lista toda a fila de espera do salão (Gestão).
     */
    List<Waitlist> findAllByProviderId(UUID providerId);
    
    void delete(UUID id);
}