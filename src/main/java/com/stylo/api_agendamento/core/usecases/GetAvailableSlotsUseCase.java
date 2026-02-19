package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class GetAvailableSlotsUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;
    private final IServiceProviderRepository providerRepository;

    public List<LocalTime> execute(Input input) {
        // 1. Validar Profissional e Estabelecimento
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        ServiceProvider provider = providerRepository.findById(professional.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 2. Cálculo de Carga Horária e Duração
        professional.validateCanPerform(input.services());
        int totalDuration = input.services().stream().mapToInt(Service::getDuration).sum();

        // 3. Obter configuração do dia (Expediente)
        DailyAvailability dailyConfig = professional.getAvailability().stream()
                .filter(a -> a.dayOfWeek() == input.date().getDayOfWeek())
                .findFirst()
                .orElse(null);

        if (dailyConfig == null || !dailyConfig.isOpen()) {
            return new ArrayList<>();
        }

        // 4. Buscar Ocupação Real (Agendamentos e Bloqueios)
        List<Appointment> existingOccupations = appointmentRepository
                .findAllByProfessionalIdAndDate(input.professionalId(), input.date())
                .stream()
                .filter(app -> app.getStatus() != AppointmentStatus.CANCELLED)
                .collect(Collectors.toList());

        // 5. Motor de Geração de Slots
        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime currentSlot = dailyConfig.startTime();
        
        // Ajuste de fuso horário para "hoje"
        ZoneId zoneId = ZoneId.of(provider.getTimeZone());
        LocalTime nowInProviderZone = LocalTime.now(zoneId);
        LocalDate todayInProviderZone = LocalDate.now(zoneId);
        
        // O último horário possível deve comportar a duração total dos serviços
        LocalTime lastPossibleSlot = dailyConfig.endTime().minusMinutes(totalDuration);

        while (!currentSlot.isAfter(lastPossibleSlot)) {
            LocalTime slotStart = currentSlot;
            LocalTime slotEnd = currentSlot.plusMinutes(totalDuration);

            // Regra 1: Não permitir horários que já passaram hoje
            boolean isPast = input.date().isEqual(todayInProviderZone) && slotStart.isBefore(nowInProviderZone);

            // Regra 2: Verificar sobreposição com qualquer ocupação existente
            boolean hasConflict = existingOccupations.stream().anyMatch(occ -> {
                LocalTime occStart = occ.getStartTime().toLocalTime();
                LocalTime occEnd = occ.getEndTime().toLocalTime();
                // (StartA < EndB) AND (EndA > StartB) -> Sobreposição detectada
                return slotStart.isBefore(occEnd) && slotEnd.isAfter(occStart);
            });

            if (!hasConflict && !isPast) {
                availableSlots.add(slotStart);
            }

            // Incrementa o slot conforme o intervalo configurado (ex: a cada 30 min)
            currentSlot = currentSlot.plusMinutes(professional.getSlotInterval());
        }

        return availableSlots;
    }

    public record Input(
            UUID professionalId,
            LocalDate date,
            List<Service> services
    ) {}
}