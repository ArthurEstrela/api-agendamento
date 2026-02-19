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
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetProfessionalProfileUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IReviewRepository reviewRepository;

    public ProfessionalProfile execute(UUID professionalId) {
        // 1. Busca o profissional e valida existência
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 2. Busca a média de avaliações
        Double averageRating = reviewRepository.getAverageRatingByProfessional(professionalId);

        // 3. Busca os feedbacks recentes (limitado a 10 itens para performance)
        // O PagedResult garante que não carregaremos milhares de reviews no perfil
        List<Review> recentReviews = reviewRepository.findAllByProfessionalId(professionalId, 0, 10).items();

        // 4. Retorna o DTO completo para o Front-end
        return new ProfessionalProfile(
                professional.getId(),
                professional.getName(),
                professional.getAvatarUrl(),
                professional.getBio(),
                professional.getSpecialties(), // ✨ As tags (ex: Visagista)
                professional.getServices(), // ✨ Os serviços que ele presta (ex: Corte Tesoura)
                averageRating != null ? averageRating : 0.0,
                recentReviews);
    }
}