package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetProfessionalAvailabilityUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;
    private final IServiceRepository serviceRepository;
    private final IServiceProviderRepository providerRepository;

    public List<LocalTime> execute(UUID professionalId, LocalDate date, List<UUID> serviceIds) {
        // 1. Buscas e Validações de Contexto
        Professional prof = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        ServiceProvider provider = providerRepository.findById(prof.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        List<Service> services = serviceRepository.findAllByIds(serviceIds);
        if (services.isEmpty()) {
            throw new BusinessException("Selecione ao menos um serviço.");
        }
        
        int totalDuration = services.stream().mapToInt(Service::getDuration).sum();

        // 2. Verifica se o profissional atende no dia solicitado
        DailyAvailability availability = prof.getAvailability().stream()
                .filter(a -> a.dayOfWeek() == date.getDayOfWeek() && a.isOpen())
                .findFirst()
                .orElseThrow(() -> new BusinessException("O profissional não atende nesta data."));

        // ✨ CORREÇÃO 1: Fail-Fast se a duração dos serviços for maior que o expediente do dia
        long shiftDurationMinutes = Duration.between(availability.startTime(), availability.endTime()).toMinutes();
        if (totalDuration > shiftDurationMinutes) {
            throw new BusinessException(String.format(
                "A duração total dos serviços (%d min) ultrapassa o tempo total de expediente do profissional neste dia (%d min).", 
                totalDuration, shiftDurationMinutes
            ));
        }

        // ✨ CORREÇÃO 2: Busca ocupações explícitas (Agendamentos e Bloqueios Manuais)
        List<Appointment> occupations = appointmentRepository.findAllByProfessionalIdAndDate(professionalId, date)
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING || 
                             a.getStatus() == AppointmentStatus.SCHEDULED || 
                             a.getStatus() == AppointmentStatus.BLOCKED) // Explicita que Bloqueios importam
                .toList();

        // 4. Configuração de Horário Local
        ZoneId zoneId = ZoneId.of(provider.getTimeZone());
        LocalTime nowInProviderZone = LocalTime.now(zoneId);
        LocalDate todayInProviderZone = LocalDate.now(zoneId);

        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime currentSlot = availability.startTime();
        
        // O limite para o início do serviço é o horário de fechar menos a duração total
        LocalTime lastStartTime = availability.endTime().minusMinutes(totalDuration);

        while (!currentSlot.isAfter(lastStartTime)) {
            LocalTime slotEnd = currentSlot.plusMinutes(totalDuration);
            
            // Validação de Tempo Passado (Se for para hoje)
            boolean isPast = date.isEqual(todayInProviderZone) && currentSlot.isBefore(nowInProviderZone);
            
            // Validação de Conflito de Agenda com Ocupações e Bloqueios
            final LocalTime finalCurrent = currentSlot;
            boolean hasConflict = occupations.stream().anyMatch(occ -> {
                LocalTime start = occ.getStartTime().toLocalTime();
                LocalTime end = occ.getEndTime().toLocalTime();
                // Overlap: (SlotStart < OccEnd) AND (SlotEnd > OccStart)
                return finalCurrent.isBefore(end) && slotEnd.isAfter(start);
            });

            if (!isPast && !hasConflict) {
                availableSlots.add(currentSlot);
            }

            // Pula para o próximo slot baseado no intervalo configurado no profissional (ex: a cada 30 min)
            currentSlot = currentSlot.plusMinutes(prof.getSlotInterval() != null ? prof.getSlotInterval() : 30);
        }

        return availableSlots;
    }
}