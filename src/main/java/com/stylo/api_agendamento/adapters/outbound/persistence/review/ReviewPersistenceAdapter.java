package com.stylo.api_agendamento.adapters.outbound.persistence.review;

import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReviewPersistenceAdapter implements IReviewRepository {

    private final JpaReviewRepository jpaReviewRepository;
    private final ReviewMapper reviewMapper;

    @Override
    public Review save(Review review) {
        var entity = reviewMapper.toEntity(review);
        var savedEntity = jpaReviewRepository.save(entity);
        return reviewMapper.toDomain(savedEntity);
    }

    @Override
    public List<Review> findByProfessionalId(String professionalId) {
        return jpaReviewRepository.findAllByProfessionalId(UUID.fromString(professionalId))
                .stream()
                .map(reviewMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Review> findByServiceProviderId(String providerId) {
        return jpaReviewRepository.findAllByServiceProviderId(UUID.fromString(providerId))
                .stream()
                .map(reviewMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Review> findAllByProviderId(String providerId) {
        // Geralmente findAllByProviderId e findByServiceProviderId fazem a mesma coisa no seu contrato
        return findByServiceProviderId(providerId);
    }

    @Override
    public Double getAverageRating(String professionalId) {
        Double avg = jpaReviewRepository.getAverageRatingByProfessionalId(UUID.fromString(professionalId));
        return avg != null ? avg : 0.0;
    }

    @Override
    public boolean existsByAppointmentId(String appointmentId) {
        return jpaReviewRepository.existsByAppointmentId(UUID.fromString(appointmentId));
    }
}