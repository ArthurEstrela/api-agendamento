package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class GetAvailableSlotsUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;

    public List<LocalTime> execute(AvailableSlotsInput input) {
        // 1. Buscar o profissional e validar existência
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Validar se o profissional realiza os serviços solicitados
        professional.validateCanPerform(input.services());

        // 3. Calcular a duração total necessária
        int totalDuration = input.services().stream()
                .mapToInt(Service::getDuration)
                .sum();

        // 4. Obter a configuração de disponibilidade para o dia da semana
        DailyAvailability dailyConfig = professional.getAvailability().stream()
                .filter(a -> a.dayOfWeek() == input.date().getDayOfWeek())
                .findFirst()
                .orElse(null);

        if (dailyConfig == null || !dailyConfig.isOpen()) {
            return new ArrayList<>();
        }

        // 5. Buscar agendamentos e BLOQUEIOS existentes (Ocupação Real)
        // Melhoria: Consideramos SCHEDULED, PENDING e o novo status BLOCKED como impeditivos
        List<Appointment> existingOccupations = appointmentRepository
                .findAllByProfessionalIdAndDate(input.professionalId(), input.date())
                .stream()
                .filter(app -> app.getStatus() != AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        // 6. Gerar slots baseados no slotInterval
        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime currentSlot = dailyConfig.startTime();
        LocalTime now = LocalTime.now();
        LocalDate today = LocalDate.now();
        
        LocalTime lastPossibleSlot = dailyConfig.endTime().minusMinutes(totalDuration);

        while (!currentSlot.isAfter(lastPossibleSlot)) {
            LocalTime slotStart = currentSlot;
            LocalTime slotEnd = currentSlot.plusMinutes(totalDuration);

            // MELHORIA MONSTRA: Não permitir horários que já passaram se a data for hoje
            boolean isPastTime = input.date().isEqual(today) && slotStart.isBefore(now);

            // Verificar conflito com agendamentos ou bloqueios administrativos
            boolean hasConflict = existingOccupations.stream().anyMatch(app -> {
                LocalTime appStart = app.getStartTime().toLocalTime();
                LocalTime appEnd = app.getEndTime().toLocalTime();
                return slotStart.isBefore(appEnd) && slotEnd.isAfter(appStart);
            });

            if (!hasConflict && !isPastTime) {
                availableSlots.add(slotStart);
            }

            currentSlot = currentSlot.plusMinutes(professional.getSlotInterval());
        }

        return availableSlots;
    }

    public record AvailableSlotsInput(
        String professionalId,
        LocalDate date,
        List<Service> services
    ) {}
}