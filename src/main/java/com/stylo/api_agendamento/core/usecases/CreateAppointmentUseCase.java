package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CreateAppointmentUseCase {
    private final IAppointmentRepository repository;
    private final INotificationProvider notificationProvider;

    public Appointment execute(Appointment appointment) {
        // Validação de conflito de horário
        if (repository.hasConflictingAppointment(
                appointment.getProfessionalId(), 
                appointment.startTime, 
                appointment.endTime)) {
            throw new RuntimeException("Profissional já possui agendamento neste horário.");
        }

        appointment.setId(UUID.randomUUID().toString());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setCreatedAt(LocalDateTime.now());

        Appointment saved = repository.save(appointment);

        notificationProvider.send(
            appointment.getProviderId(),
            "Novo Agendamento",
            "Você tem um novo agendamento para " + appointment.getStartTime()
        );

        return saved;
    }
}