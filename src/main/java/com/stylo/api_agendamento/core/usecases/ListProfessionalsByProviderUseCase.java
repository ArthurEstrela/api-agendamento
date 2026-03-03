package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import com.stylo.api_agendamento.core.usecases.dto.ProfessionalProfile;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.DailyAvailabilityDTO; // ✨ Import do DTO
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ListProfessionalsByProviderUseCase {

    private final IProfessionalRepository professionalRepository;
    
    // ✨ Substituímos o GetProfessionalProfileUseCase pelo IReviewRepository
    // Isso evita o problema de N+1 (consultas duplicadas no banco para buscar o mesmo profissional)
    private final IReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<ProfessionalProfile> execute(UUID providerId) {
        log.info("Buscando lista de profissionais ativos para o estabelecimento ID: {}", providerId);

        // 1. Busca as entidades (1 única consulta no banco para trazer todos os profissionais!)
        List<Professional> professionals = professionalRepository.findByServiceProviderId(providerId);

        // 2. Filtra os ativos e mapeia montando o perfil diretamente
        List<ProfessionalProfile> profiles = professionals.stream()
                .filter(p -> Boolean.TRUE.equals(p.isActive())) // Proteção contra null e garante apenas ativos
                .map(professional -> {
                    // Busca as avaliações (rating e reviews)
                    Double averageRating = reviewRepository.getAverageRatingByProfessional(professional.getId());
                    List<Review> recentReviews = reviewRepository.findAllByProfessionalId(professional.getId(), 0, 10).items();

                    // ✨ Mapeia a disponibilidade do domínio para o DTO (Formato HH:mm para o React)
                    List<DailyAvailabilityDTO> availabilitiesDTO = professional.getAvailability().stream()
                            .map(DailyAvailabilityDTO::fromDomain)
                            .collect(Collectors.toList());

                    // Constrói o DTO usando os dados que já estão na memória
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
                            availabilitiesDTO // ✨ Passando a disponibilidade corretamente
                    );
                })
                .collect(Collectors.toList());

        log.info("Encontrados {} profissionais ativos para o estabelecimento ID: {}", profiles.size(), providerId);

        return profiles;
    }
}