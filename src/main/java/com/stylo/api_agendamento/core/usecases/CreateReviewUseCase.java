package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Review;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IReviewRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
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
    // ✨ INJEÇÃO DA PORTA DO ESTABELECIMENTO
    private final IServiceProviderRepository providerRepository;
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

        // 4. Criação da Review via Domínio
        Review review = Review.create(
                appointment, 
                input.rating(), 
                input.comment()
        );
        Review savedReview = reviewRepository.save(review);

        // 5. ✨ ATUALIZAÇÃO DO RANKING DO ESTABELECIMENTO (Desnormalização de alta performance)
        ServiceProvider provider = providerRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado para atualizar a nota."));
        
        // Delega para o Domínio o recálculo matemático da média
        provider.updateRating(input.rating());
        
        // Salva o Provider atualizado com a nova nota
        providerRepository.save(provider);

        log.info("Nova avaliação recebida no estabelecimento {}. Nota recebida: {}. Nova média geral: {}", 
                provider.getId(), input.rating(), provider.getAverageRating());

        return savedReview;
    }

    public record Input(UUID appointmentId, int rating, String comment) {}
}