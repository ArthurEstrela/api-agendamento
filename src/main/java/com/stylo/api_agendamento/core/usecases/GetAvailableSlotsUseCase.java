package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetAvailableSlotsUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IUserRepository userRepository;

    public List<LocalTime> execute(String professionalId, LocalDate date) {
        // 1. Buscar o profissional e suas configurações
        Professional professional = (Professional) userRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado"));

        // 2. Identificar a disponibilidade para o dia da semana (ex: SEGUNDA)
        String dayOfWeek = date.getDayOfWeek().name();
        DailyAvailability availability = professional.getAvailability().stream()
                .filter(a -> a.getDayOfWeek().equalsIgnoreCase(dayOfWeek) && a.isOpen())
                .findFirst()
                .orElse(null);

        // Se o profissional não atende nesse dia, retorna lista vazia
        if (availability == null) return new ArrayList<>();

        // 3. Buscar agendamentos já existentes para o dia (PENDING ou CONFIRMED)
        List<Appointment> existingAppointments = appointmentRepository
                .findByProfessionalIdAndDate(professionalId, date)
                .stream()
                .filter(app -> app.getStatus() == AppointmentStatus.CONFIRMED 
                            || app.getStatus() == AppointmentStatus.PENDING)
                .collect(Collectors.toList());

        // 4. Gerar todos os slots possíveis baseados no slotInterval
        return calculateAvailableSlots(availability, existingAppointments, professional.getSlotInterval());
    }

    private List<LocalTime> calculateAvailableSlots(
            DailyAvailability availability, 
            List<Appointment> appointments, 
            Integer interval) {
        
        List<LocalTime> slots = new ArrayList<>();
        LocalTime currentTime = availability.getStartTime();
        LocalTime endTime = availability.getEndTime();
        int slotDuration = (interval != null && interval > 0) ? interval : 30;

        while (currentTime.plusMinutes(slotDuration).isBefore(endTime) || currentTime.plusMinutes(slotDuration).equals(endTime)) {
            LocalTime finalCurrentTime = currentTime;
            
            // Verifica se o slot atual conflita com algum agendamento existente
            boolean isBusy = appointments.stream().anyMatch(app -> 
                (finalCurrentTime.isBefore(app.getEndTime()) && finalCurrentTime.plusMinutes(slotDuration).isAfter(app.getStartTime()))
            );

            if (!isBusy) {
                slots.add(finalCurrentTime);
            }

            currentTime = currentTime.plusMinutes(slotDuration);
        }

        return slots;
    }
}