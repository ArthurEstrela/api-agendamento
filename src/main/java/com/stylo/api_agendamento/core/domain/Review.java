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
    private final String professionalName; // Importante para o histórico
    private final int rating; 
    private final String comment; // Tornar final reforça a imutabilidade do feedback
    private final LocalDateTime createdAt;

    public static Review create(Appointment appointment, int rating, String comment) {
        // REGRA DE NEGÓCIO: Só avalia o que foi concluído
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessException("Apenas agendamentos finalizados podem ser avaliados.");
        }

        // Validação de nota
        if (rating < 1 || rating > 5) {
            throw new BusinessException("A avaliação deve ser entre 1 e 5 estrelas.");
        }

        return Review.builder()
                .appointmentId(appointment.getId())
                .clientId(appointment.getClientId())
                .clientName(appointment.getClientName()) // Pegando do objeto Appointment
                .serviceProviderId(appointment.getProviderId())
                .professionalId(appointment.getProfessionalId())
                .professionalName(appointment.getProfessionalName()) // Pegando do objeto Appointment
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();
    }
}