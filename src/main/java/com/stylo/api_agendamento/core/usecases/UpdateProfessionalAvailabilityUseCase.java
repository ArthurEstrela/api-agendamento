package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class UpdateProfessionalAvailabilityUseCase {

        private final IProfessionalRepository professionalRepository;

        public record Input(UUID professionalId, List<DailyAvailability> availabilities, Integer slotInterval) {
        }

        @Transactional 
        public void execute(Input input) {
                Professional professional = professionalRepository.findById(input.professionalId())
                                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

                // Atualiza a grade de dias
                professional.updateAvailability(input.availabilities());

                // ✨ Usa o método de domínio correto no lugar do Setter
                if (input.slotInterval() != null) {
                        professional.updateSlotInterval(input.slotInterval());
                }

                professionalRepository.save(professional);
        }
}