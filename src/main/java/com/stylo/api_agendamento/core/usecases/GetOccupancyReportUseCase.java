package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.usecases.dto.OccupancyReport;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GetOccupancyReportUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;

    public OccupancyReport execute(String providerId, LocalDate startDate, LocalDate endDate) {
        // 1. Busca todos os profissionais do estabelecimento
        List<Professional> professionals = professionalRepository.findAllByProviderId(providerId);
        
        // 2. Busca todos os agendamentos do período
        List<Appointment> allAppointments = appointmentRepository.findAllByProviderIdAndPeriod(
                providerId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

        // 3. Agrupa agendamentos por profissional para evitar loops N+1
        Map<String, List<Appointment>> appointmentsByProf = allAppointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .collect(Collectors.groupingBy(Appointment::getProfessionalId));

        List<OccupancyReport.ProfessionalOccupancy> rankings = new ArrayList<>();

        for (Professional prof : professionals) {
            long availableMinutes = calculateTotalAvailableMinutes(prof, startDate, endDate);
            
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
                    Math.round(percentage * 100.0) / 100.0 // Arredonda 2 casas
            ));
        }

        double average = rankings.stream()
                .mapToDouble(OccupancyReport.ProfessionalOccupancy::occupancyPercentage)
                .average()
                .orElse(0);

        return new OccupancyReport(rankings, Math.round(average * 100.0) / 100.0);
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
                    .orElse(0);
            current = current.plusDays(1);
        }
        return total;
    }
}