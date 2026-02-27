package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class DeleteProfessionalUseCase {

    private final IProfessionalRepository professionalRepository;

    @Transactional
    public void execute(UUID professionalId) {
        // 1. Busca o profissional no banco
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado"));

        // 2. Executa a regra de negócio do Soft Delete (muda isActive para false)
        professional.deactivate();

        // 3. Salva a alteração no banco
        professionalRepository.save(professional);
    }
}