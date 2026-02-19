package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.usecases.dto.OccupancyReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetOccupancyReportUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;

    public OccupancyReport execute(UUID providerId, LocalDate startDate, LocalDate endDate) {
        // 1. Busca todos os profissionais e agendamentos do período (Escopo do Estabelecimento)
        List<Professional> professionals = professionalRepository.findAllByProviderId(providerId);
        List<Appointment> allAppointments = appointmentRepository.findAllByProviderIdAndPeriod(
                providerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

        // 2. Agrupa por profissional para performance O(N)
        Map<UUID, List<Appointment>> appointmentsByProf = allAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .collect(Collectors.groupingBy(Appointment::getProfessionalId));

        List<OccupancyReport.ProfessionalOccupancy> rankings = new ArrayList<>();

        for (Professional prof : professionals) {
            // Calcula minutos disponíveis baseado na grade de horários do profissional
            long availableMinutes = calculateTotalAvailableMinutes(prof, startDate, endDate);
            
            // Calcula minutos efetivamente ocupados (Agendamentos e Bloqueios)
            List<Appointment> profApps = appointmentsByProf.getOrDefault(prof.getId(), List.of());
            long occupiedMinutes = profApps.stream()
                    .mapToLong(a -> Duration.between(a.getStartTime(), a.getEndTime()).toMinutes())
                    .sum();

            double percentage = availableMinutes > 0 
                    ? (double) occupiedMinutes / availableMinutes * 100 
                    : 0;

            rankings.add(new OccupancyReport.ProfessionalOccupancy(
                    prof.getId(),
                    prof.getName(),
                    (int) availableMinutes,
                    (int) occupiedMinutes,
                    Math.min(100.0, Math.round(percentage * 100.0) / 100.0) // Capado em 100%
            ));
        }

        double averageOccupancy = rankings.stream()
                .mapToDouble(OccupancyReport.ProfessionalOccupancy::occupancyPercentage)
                .average()
                .orElse(0);

        return new OccupancyReport(rankings, Math.round(averageOccupancy * 100.0) / 100.0);
    }

    private long calculateTotalAvailableMinutes(Professional prof, LocalDate start, LocalDate end) {
        long total = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            final LocalDate date = current;
            total += prof.getAvailability().stream()
                    .filter(a -> a.dayOfWeek() == date.getDayOfWeek() && a.isOpen())
                    .mapToLong(a -> Duration.between(a.startTime(), a.endTime()).toMinutes())
                    .findFirst()
                    .orElse(0L);
            current = current.plusDays(1);
        }
        return total;
    }
}