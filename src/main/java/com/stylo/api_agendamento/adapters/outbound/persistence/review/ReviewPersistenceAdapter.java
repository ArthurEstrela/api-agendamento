package com.stylo.api_agendamento.adapters.outbound.persistence.review;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Review> findById(UUID id) {
        return jpaReviewRepository.findById(id)
                .map(reviewMapper::toDomain);
    }

    @Override
    public PagedResult<Review> findAllByProfessionalId(UUID professionalId, int page, int size) {
        // Ordena pelas mais recentes primeiro
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewEntity> entityPage = jpaReviewRepository.findAllByProfessionalId(professionalId, pageable);

        return toPagedResult(entityPage);
    }

    @Override
    public PagedResult<Review> findAllByProviderId(UUID providerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewEntity> entityPage = jpaReviewRepository.findAllByServiceProviderId(providerId, pageable);

        return toPagedResult(entityPage);
    }

    @Override
    public Double getAverageRatingByProfessional(UUID professionalId) {
        Double avg = jpaReviewRepository.getAverageRatingByProfessionalId(professionalId);
        return avg != null ? avg : 0.0;
    }

    @Override
    public boolean existsByAppointmentId(UUID appointmentId) {
        return jpaReviewRepository.existsByAppointmentId(appointmentId);
    }

    // --- Método Auxiliar de Mapeamento de Página ---
    private PagedResult<Review> toPagedResult(Page<ReviewEntity> entityPage) {
        List<Review> domainItems = entityPage.getContent().stream()
                .map(reviewMapper::toDomain)
                .toList();

        return new PagedResult<>(
                domainItems,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }
}