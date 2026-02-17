package com.stylo.api_agendamento.adapters.outbound.persistence.waitlist;

import com.stylo.api_agendamento.core.domain.Waitlist;
import com.stylo.api_agendamento.core.ports.IWaitlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WaitlistPersistenceAdapter implements IWaitlistRepository {

    private final JpaWaitlistRepository repository;
    private final WaitlistMapper mapper;

    @Override
    public Waitlist save(Waitlist waitlist) {
        WaitlistEntity entity = mapper.toEntity(waitlist);
        WaitlistEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<Waitlist> findAllByProfessionalAndDate(String professionalId, LocalDate date) {
        return repository.findActiveByProfessionalAndDate(UUID.fromString(professionalId), date)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}