package com.stylo.api_agendamento.core.ports;

import java.util.List;

import com.stylo.api_agendamento.core.domain.Review;

public interface IReviewRepository {
    Review save(Review review);
    List<Review> findByProfessionalId(String professionalId);
    List<Review> findByServiceProviderId(String providerId);
    Double getAverageRating(String professionalId);
    boolean existsByAppointmentId(String appointmentId);
    List<Review> findAllByProviderId(String providerId);
}