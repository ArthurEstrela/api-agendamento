package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.Waitlist;
import java.time.LocalDate;
import java.util.List;

public interface IWaitlistRepository {
    Waitlist save(Waitlist waitlist);
    List<Waitlist> findAllByProfessionalAndDate(String professionalId, LocalDate date);
}