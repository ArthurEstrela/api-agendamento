package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RescheduleAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceProviderRepository serviceProviderRepository;
    private final INotificationProvider notificationProvider;

    @Transactional
    public Appointment execute(Input input) {
        // 1. Busca Agendamento e valida estado
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus().isTerminalState()) {
            throw new BusinessException("Não é possível reagendar um serviço já concluído ou cancelado.");
        }

        // 2. Valida política de alteração do Estabelecimento
        ServiceProvider provider = serviceProviderRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));
        
        // O domínio valida se o cliente está dentro do prazo (ex: 2h antes) para reagendar
        provider.validateCancellationPolicy(appointment.getStartTime());

        // 3. Valida disponibilidade do Profissional
        Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        int totalDuration = appointment.calculateTotalDuration();
        
        if (!professional.isAvailable(input.newStartTime(), totalDuration)) {
            throw new BusinessException("O profissional não possui disponibilidade nesta nova data/horário.");
        }

        // 4. Executa a mudança no Domínio
        // Isso recalcula endTime e reseta confirmações se necessário
        appointment.reschedule(input.newStartTime());

        // 5. Proteção Atômica contra Double-Booking
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                appointment.getProfessionalId(),
                appointment.getStartTime(),
                appointment.getEndTime()
        );

        if (hasConflict) {
            throw new BusinessException("Este horário acabou de ser ocupado por outro cliente.");
        }

        // 6. Persistência
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        
        // 7. Notificações contextuais
        notifyReschedule(updatedAppointment);

        log.info("Agendamento {} reagendado com sucesso para {}", appointment.getId(), input.newStartTime());
        return updatedAppointment;
    }

    private void notifyReschedule(Appointment appt) {
        try {
            String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"));
            String message = String.format("Seu agendamento de %s foi alterado para %s.", 
                    appt.getServicesSnapshot(), dateFormatted);
            
            notificationProvider.sendPushNotification(appt.getClientId(), "📅 Horário Alterado", message, "/my-appointments");
        } catch (Exception e) {
            log.error("Erro não-bloqueante na notificação de reagendamento: {}", e.getMessage());
        }
    }

    public record Input(UUID appointmentId, LocalDateTime newStartTime) {}
}