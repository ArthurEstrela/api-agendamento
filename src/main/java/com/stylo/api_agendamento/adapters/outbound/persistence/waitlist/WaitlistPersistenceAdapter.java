package com.stylo.api_agendamento.adapters.outbound.persistence.waitlist;

import com.stylo.api_agendamento.core.domain.Waitlist;
import com.stylo.api_agendamento.core.ports.IWaitlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Waitlist> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Waitlist> findAllByProfessionalIdAndDate(UUID professionalId, LocalDate date) {
        return repository.findAllByProfessionalIdAndDesiredDateAndNotifiedFalseOrderByRequestTimeAsc(professionalId, date)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Waitlist> findAllByProviderId(UUID providerId) {
        return repository.findAllByServiceProviderIdAndNotifiedFalseOrderByRequestTimeDesc(providerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}