package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class UpdateProfessionalAvailabilityUseCase {

    private final IProfessionalRepository professionalRepository;

    @Transactional
    public void execute(Input input) {
        // 1. Busca o profissional e valida existência
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 2. Atualiza a grade no domínio (O domínio valida intersecções e horários inválidos)
        professional.updateAvailability(input.availabilities());

        // 3. Persistência
        professionalRepository.save(professional);
        
        log.info("Grade de horários atualizada para o profissional: {} (ID: {})", 
                professional.getName(), professional.getId());
    }

    public record Input(
            UUID professionalId,
            List<DailyAvailability> availabilities
    ) {}
}