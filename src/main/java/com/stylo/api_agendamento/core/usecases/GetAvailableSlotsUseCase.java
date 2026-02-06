package com.stylo.api_agendamento.core.usecases;

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
            return new ArrayList<>(); // Estabelecimento fechado neste dia
        }

        // 5. Buscar agendamentos existentes do profissional para o dia (não cancelados)
        List<Appointment> existingAppointments = appointmentRepository
                .findAllByProfessionalIdAndDate(input.professionalId(), input.date())
                .stream()
                .filter(app -> app.getStatus() != AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        // 6. Gerar slots baseados no slotInterval (ex: de 30 em 30 min)
        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime currentSlot = dailyConfig.startTime();
        
        // O último slot possível deve permitir a execução do serviço total antes de fechar
        LocalTime lastPossibleSlot = dailyConfig.endTime().minusMinutes(totalDuration);

        while (!currentSlot.isAfter(lastPossibleSlot)) {
            LocalTime slotStart = currentSlot;
            LocalTime slotEnd = currentSlot.plusMinutes(totalDuration);

            // Verificar se este intervalo (Início -> Fim do Serviço) conflita com algum agendamento
            boolean hasConflict = existingAppointments.stream().anyMatch(app -> {
                LocalTime appStart = app.getStartTime().toLocalTime();
                LocalTime appEnd = app.getEndTime().toLocalTime();
                
                // Lógica de sobreposição: (Início1 < Fim2) E (Fim1 > Início2)
                return slotStart.isBefore(appEnd) && slotEnd.isAfter(appStart);
            });

            if (!hasConflict) {
                availableSlots.add(slotStart);
            }

            // Pula para o próximo slot configurado (ex: 30 min)
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