package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class UpdateProfessionalAvailabilityUseCase {

    private final IProfessionalRepository professionalRepository;

    public void execute(UpdateAvailabilityInput input) {
        // 1. Busca o profissional e valida existência
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 2. Regra de Negócio: Atualiza a grade no objeto de domínio (Valida internamente)
        professional.updateAvailability(input.availabilities());

        // 3. Persistência
        professionalRepository.save(professional);
    }

    public record UpdateAvailabilityInput(
            String professionalId,
            List<DailyAvailability> availabilities
    ) {}
}