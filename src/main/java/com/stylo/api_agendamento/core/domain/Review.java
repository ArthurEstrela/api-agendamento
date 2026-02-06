package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Review {
    private final String id;
    private final String appointmentId;
    private final String clientId;
    private final String clientName;
    private final String serviceProviderId;
    private final String professionalId;
    private final String professionalName;
    private final int rating; 
    private String comment;
    private final LocalDateTime createdAt;

    public static Review create(String appointmentId, String clientId, String providerId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BusinessException("A avaliação deve ser entre 1 e 5 estrelas.");
        }
        return Review.builder()
                .appointmentId(appointmentId)
                .clientId(clientId)
                .serviceProviderId(providerId)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();
    }
}