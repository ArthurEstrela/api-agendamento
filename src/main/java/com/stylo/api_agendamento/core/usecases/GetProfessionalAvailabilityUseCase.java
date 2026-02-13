package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class GetProfessionalAvailabilityUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;
    private final IServiceRepository serviceRepository;

    public List<LocalTime> execute(String professionalId, LocalDate date, List<String> serviceIds) {
        Professional prof = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        List<Service> services = serviceRepository.findAllByIds(serviceIds);
        int totalDuration = services.stream().mapToInt(Service::getDuration).sum();

        // Busca disponibilidade do dia da semana
        DailyAvailability availability = prof.getAvailability().stream()
                .filter(a -> a.dayOfWeek() == date.getDayOfWeek() && a.isOpen())
                .findFirst()
                .orElseThrow(() -> new BusinessException("Profissional não atende nesta data."));

        // Busca ocupações existentes (Agendamentos e Bloqueios)
        List<Appointment> occupations = appointmentRepository.findAllByProfessionalIdAndDate(professionalId, date)
                .stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .toList();

        List<LocalTime> slots = new ArrayList<>();
        LocalTime current = availability.startTime();
        LocalTime now = LocalTime.now();
        
        // O último slot possível deve permitir a execução do serviço completo antes de fechar
        LocalTime limit = availability.endTime().minusMinutes(totalDuration);

        while (!current.isAfter(limit)) {
            LocalTime slotEnd = current.plusMinutes(totalDuration);
            
            // Validações: 
            // 1. Não pode ser no passado (se for hoje)
            boolean isPast = date.isEqual(LocalDate.now()) && current.isBefore(now);
            
            // 2. Não pode sobrepor nenhum agendamento existente
            LocalTime finalCurrent = current;
            boolean hasConflict = occupations.stream().anyMatch(occ -> {
                LocalTime start = occ.getStartTime().toLocalTime();
                LocalTime end = occ.getEndTime().toLocalTime();
                return finalCurrent.isBefore(end) && slotEnd.isAfter(start);
            });

            if (!isPast && !hasConflict) {
                slots.add(current);
            }

            current = current.plusMinutes(prof.getSlotInterval());
        }

        return slots;
    }
}