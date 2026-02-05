package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.ports.*;
import com.stylo.api_agendamento.core.exceptions.*;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final GetAvailableSlotsUseCase getAvailableSlotsUseCase;

    public Appointment execute(Appointment appointment) {
        // 1. Validar se o horário escolhido ainda está disponível
        List<LocalTime> availableSlots = getAvailableSlotsUseCase.execute(
            appointment.getProfessionalId(), 
            appointment.getDate()
        );

        boolean isSlotAvailable = availableSlots.contains(appointment.getStartTime());

        if (!isSlotAvailable) {
            throw new ScheduleConflictException("O horário selecionado não está mais disponível.");
        }

        // 2. Definir status inicial do agendamento
        // Em um SaaS robusto, o status inicial pode depender se o profissional exige aprovação manual
        appointment.setStatus(AppointmentStatus.PENDING);

        // 3. Persistir
        return appointmentRepository.save(appointment);
    }
}