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

        public record Input(UUID professionalId, List<DailyAvailability> availabilities) {
        }

        @Transactional // ✨ EXTREMAMENTE IMPORTANTE: Garante que as mudanças sejam salvas no banco
        public void execute(Input input) {
                Professional professional = professionalRepository.findById(input.professionalId())
                                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

                // ✨ CORREÇÃO AQUI: Chama o método de domínio seguro em vez de um setter padrão
                professional.updateAvailability(input.availabilities());

                // 2. ✨ SALVA NO BANCO DE DADOS: Se não tiver isso, a atualização é perdida
                professionalRepository.save(professional);
        }
}