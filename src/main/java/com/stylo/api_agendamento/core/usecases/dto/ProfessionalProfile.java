package com.stylo.api_agendamento.core.usecases.dto;

import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.domain.Service;
import java.util.List;
import java.util.UUID;

public record ProfessionalProfile(
                UUID id,
                String name,
                String email,
                boolean isOwner,
                String avatarUrl,
                String bio,
                List<String> specialties, // ✨ CORRIGIDO AQUI: Mudou de String para List<String>
                List<Service> services,
                Double averageRating,
                List<Review> recentReviews) {
}