package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateReviewUseCase {

    private final IReviewRepository reviewRepository;
    private final IAppointmentRepository appointmentRepository;
    private final IUserContext userContext;

    @Transactional
    public Review execute(Input input) {
        UUID currentUserId = userContext.getCurrentUserId();

        // 1. Busca o agendamento original
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Validações de Segurança e Regra de Negócio
        if (!appointment.getClientId().equals(currentUserId)) {
            throw new BusinessException("Você só pode avaliar serviços que você mesmo realizou.");
        }

        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessException("Você só pode avaliar um serviço após a conclusão do atendimento.");
        }

        // 3. Regra Anti-Spam: Um agendamento, uma única avaliação
        if (reviewRepository.existsByAppointmentId(input.appointmentId())) {
            throw new BusinessException("Este agendamento já foi avaliado anteriormente.");
        }

        // 4. Criação via Domínio (O método create já valida a nota de 1 a 5)
        Review review = Review.create(
                appointment, 
                input.rating(), 
                input.comment()
        );

        log.info("Nova avaliação recebida para o profissional {}. Nota: {}", 
                appointment.getProfessionalId(), input.rating());

        return reviewRepository.save(review);
    }

    public record Input(UUID appointmentId, int rating, String comment) {}
}