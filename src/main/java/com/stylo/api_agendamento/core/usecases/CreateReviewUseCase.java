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
        // 1. Busca o agendamento original
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Regra Anti-Spam: Um agendamento, uma avaliação
        if (reviewRepository.existsByAppointmentId(input.appointmentId())) {
            throw new BusinessException("Você já avaliou este serviço.");
        }

        // 3. Criação via Domínio (Valida status COMPLETED e range da nota 1-5)
        Review review = Review.create(appointment, input.rating(), input.comment());

        // 4. Salva o feedback
        return reviewRepository.save(review);
    }

    public record CreateReviewInput(String appointmentId, int rating, String comment) {}
}