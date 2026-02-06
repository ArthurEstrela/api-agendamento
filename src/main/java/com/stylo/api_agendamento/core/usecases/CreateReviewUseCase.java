package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CreateReviewUseCase {

    private final IReviewRepository reviewRepository;
    private final IAppointmentRepository appointmentRepository;

    public Review execute(CreateReviewInput input) {
        // 1. Busca o agendamento
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Valida se já existe uma avaliação para este agendamento (evita spam)
        if (reviewRepository.existsByAppointmentId(input.appointmentId())) {
            throw new BusinessException("Este agendamento já foi avaliado.");
        }

        // 3. Cria a instância de domínio com as regras de validação
        Review review = Review.create(appointment, input.rating(), input.comment());

        // 4. Persistência
        return reviewRepository.save(review);
    }

    public record CreateReviewInput(
            String appointmentId,
            int rating,
            String comment
    ) {}
}