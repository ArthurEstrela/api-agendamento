package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import com.stylo.api_agendamento.core.usecases.dto.ProfessionalProfile;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class GetProfessionalProfileUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IReviewRepository reviewRepository;

    public ProfessionalProfile execute(String professionalId) {
        // 1. Busca o profissional e valida existência
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 2. Busca a média de avaliações (Porta IReviewRepository)
        Double averageRating = reviewRepository.getAverageRating(professionalId);

        // 3. Busca os feedbacks mais recentes para mostrar no perfil
        List<Review> reviews = reviewRepository.findByProfessionalId(professionalId);

        // 4. Retorna o perfil completo e "mastigado" para o front-end
        return new ProfessionalProfile(
                professional.getId(),
                professional.getName(),
                professional.getAvatarUrl(),
                professional.getBio(),
                professional.getServices(),
                averageRating != null ? averageRating : 0.0,
                reviews
        );
    }
}