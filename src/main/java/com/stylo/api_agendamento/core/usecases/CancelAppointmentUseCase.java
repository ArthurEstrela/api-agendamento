package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.ports.*;
import com.stylo.api_agendamento.core.exceptions.*;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IUserRepository userRepository;
    private final INotificationProvider notificationProvider;

    public void execute(String appointmentId, String userId) {
        // 1. Buscar o agendamento
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Validar se o usuário tem permissão (Deve ser o Cliente ou o Profissional do agendamento)
        validateOwnership(appointment, userId);

        // 3. Validar Status (Só pode cancelar agendamentos que não foram concluídos ou já cancelados)
        if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException("Este agendamento não pode mais ser cancelado.");
        }

        // 4. Aplicar Política de Cancelamento (Regra de Ouro do SaaS)
        validateCancellationPolicy(appointment);

        // 5. Atualizar Status
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        // 6. Notificar a outra parte interessada
        notifyCancellation(appointment, userId);
    }

    private void validateOwnership(Appointment appointment, String userId) {
        if (!appointment.getClientId().equals(userId) && !appointment.getProfessionalId().equals(userId)) {
            throw new BusinessException("Você não tem permissão para cancelar este agendamento.");
        }
    }

    private void validateCancellationPolicy(Appointment appointment) {
        // Buscar o ServiceProvider para saber a antecedência mínima exigida
        ServiceProvider provider = (ServiceProvider) userRepository.findById(appointment.getProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Prestador de serviço não encontrado."));

        Integer minHours = provider.getCancellationMinHours();
        
        if (minHours != null && minHours > 0) {
            LocalDateTime limitTime = appointment.getStartTime().minusHours(minHours);
            if (LocalDateTime.now().isAfter(limitTime)) {
                throw new BusinessException("O prazo limite para cancelamento expirou. O mínimo é de " + minHours + " horas de antecedência.");
            }
        }
    }

    private void notifyCancellation(Appointment appointment, String initiatorId) {
        String targetId = appointment.getClientId().equals(initiatorId) 
                          ? appointment.getProfessionalId() 
                          : appointment.getClientId();
        
        notificationProvider.send(
            targetId,
            "Agendamento Cancelado",
            "O agendamento para o dia " + appointment.getStartTime() + " foi cancelado."
        );
    }
}