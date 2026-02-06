package com.stylo.api_agendamento.core.usecases.dto;

import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.domain.Service;
import java.util.List;

public record ProfessionalProfile(
    String id,
    String name,
    String avatarUrl,
    String bio,
    List<Service> services,
    Double averageRating,
    List<Review> recentReviews
) {}