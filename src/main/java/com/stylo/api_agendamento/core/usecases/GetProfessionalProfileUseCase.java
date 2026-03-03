package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import com.stylo.api_agendamento.core.usecases.dto.ProfessionalProfile;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.DailyAvailabilityDTO; // ✨ Import do DTO
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        List<Review> recentReviews = reviewRepository.findAllByProfessionalId(professionalId, 0, 10).items();

        // ✨ 4. Converte a disponibilidade do domínio para o DTO do Front-end
        List<DailyAvailabilityDTO> availabilitiesDTO = professional.getAvailability().stream()
                .map(DailyAvailabilityDTO::fromDomain)
                .collect(Collectors.toList());

        // 5. Retorna o DTO completo para o Front-end passando os 11 parâmetros
        return new ProfessionalProfile(
                professional.getId(),
                professional.getName(),
                professional.getEmail(),
                professional.isOwner(),
                professional.getAvatarUrl(),
                professional.getBio(),
                professional.getSpecialties(),
                professional.getServices(),
                averageRating,
                recentReviews,
                availabilitiesDTO // ✨ 11º parâmetro adicionado aqui!
        );
    }
}