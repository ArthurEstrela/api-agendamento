package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException; // Exceção customizada
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CreateAppointmentUseCase {
    private final IAppointmentRepository repository;
    private final INotificationProvider notificationProvider;

    public Appointment execute(Appointment appointment) {
        // 1. Validação de data retroativa
        if (appointment.getStartTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Não é possível agendar para uma data passada.");
        }

        // 2. Validação de conflito de horário usando o repositório
        if (repository.hasConflictingAppointment(
                appointment.getProfessionalId(), 
                appointment.getStartTime(), 
                appointment.getEndTime())) {
            throw new ScheduleConflictException("O profissional já possui um agendamento neste horário.");
        }

        // 3. Preparação do objeto de domínio
        appointment.setId(UUID.randomUUID().toString());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setCreatedAt(LocalDateTime.now());

        // 4. Persistência
        Appointment saved = repository.save(appointment);

        // 5. Notificação assíncrona para o prestador
        notificationProvider.send(
            appointment.getProviderId(),
            "Novo Agendamento",
            "Olá, você tem um novo agendamento para o dia " + appointment.getStartTime()
        );

        return saved;
    }
}