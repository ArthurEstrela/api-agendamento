package com.stylo.api_agendamento.core.usecases.dto;

import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.domain.Service;
import java.util.List;
import java.util.UUID;

public record ProfessionalProfile(
    UUID id,
    String name,
    String avatarUrl,
    String bio,
    List<String> specialties, // ✨ Novo campo
    List<Service> services,   // Mantendo os serviços atrelados
    Double averageRating,
    List<Review> recentReviews
) {}