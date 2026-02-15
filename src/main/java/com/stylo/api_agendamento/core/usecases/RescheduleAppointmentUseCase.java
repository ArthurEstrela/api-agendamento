package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@UseCase
@RequiredArgsConstructor
public class RescheduleAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceProviderRepository serviceProviderRepository;
    private final INotificationProvider notificationProvider;

    public Appointment execute(RescheduleInput input) {
        // 1. Busca o agendamento atual
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Busca o estabelecimento e valida política de mudança (mesma do cancelamento)
        ServiceProvider provider = serviceProviderRepository.findById(appointment.getProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));
        provider.validateCancellationPolicy(appointment.getStartTime());

        // 3. Busca o profissional para validar a nova grade de horários
        Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 4. Valida se o profissional está disponível no novo horário
        int totalDuration = appointment.getServices().stream()
                .mapToInt(s -> s.getDuration())
                .sum();
        
        if (!professional.isAvailable(input.newStartTime(), totalDuration)) {
            throw new BusinessException("O profissional não tem disponibilidade para este novo horário.");
        }

        // 5. Executa a mudança no objeto de domínio
        appointment.reschedule(input.newStartTime());

        // 6. Proteção Anti-Conflito (Double Booking) no banco de dados
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                appointment.getProfessionalId(),
                appointment.getStartTime(),
                appointment.getEndTime()
        );

        if (hasConflict) {
            throw new BusinessException("Este novo horário acabou de ser ocupado.");
        }

        // 7. Persistência e Notificação
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        
        notificationProvider.sendAppointmentRescheduled(
                appointment.getClientId(),
                "O seu agendamento foi alterado para " + input.newStartTime()
        );

        return updatedAppointment;
    }

    public record RescheduleInput(String appointmentId, LocalDateTime newStartTime) {}
}