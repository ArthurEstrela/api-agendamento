package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfirmAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final INotificationProvider notificationProvider; // Porta para disparar avisos

    public void execute(String appointmentId) {
        // 1. Busca o agendamento no banco
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado.")); //

        // 2. Executa a regra de transição de estado do Domínio
        appointment.confirm();

        // 3. Persiste a mudança
        appointmentRepository.save(appointment);

        // 4. TRIGGER MONSTRO: Notifica o cliente
        // Isso integra com seu serviço de notificações no front
        notificationProvider.sendAppointmentConfirmed(
                appointment.getClientId(),
                "Seu agendamento para " + appointment.getProfessionalName() + " foi confirmado!"
        );
    }
}