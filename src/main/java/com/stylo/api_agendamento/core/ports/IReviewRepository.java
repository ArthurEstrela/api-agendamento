package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.Review;

import java.util.Optional;
import java.util.UUID;

public interface IReviewRepository {
    
    Review save(Review review);

    Optional<Review> findById(UUID id);

    /**
     * Lista avaliações de um profissional específico (Paginado).
     */
    PagedResult<Review> findAllByProfessionalId(UUID professionalId, int page, int size);

    /**
     * Lista todas as avaliações do estabelecimento (Visão do Dono).
     */
    PagedResult<Review> findAllByProviderId(UUID providerId, int page, int size);

    /**
     * Calcula a média de estrelas (1.0 a 5.0).
     */
    Double getAverageRatingByProfessional(UUID professionalId);
    
    /**
     * Garante que o cliente só avalie uma vez por agendamento.
     */
    boolean existsByAppointmentId(UUID appointmentId);
}