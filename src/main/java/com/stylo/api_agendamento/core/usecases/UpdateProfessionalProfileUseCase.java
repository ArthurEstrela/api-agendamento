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
public class UpdateProfessionalProfileUseCase {

    private final IProfessionalRepository professionalRepository;

    @Transactional
    public Professional execute(UUID id, Input input) {
        Professional professional = professionalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado"));

        // O método updateProfile já existe na sua classe de Domínio (Professional.java)!
        professional.updateProfile(input.name(), input.bio(), null);

        return professionalRepository.save(professional);
    }

    public record Input(String name, String bio) {}
}