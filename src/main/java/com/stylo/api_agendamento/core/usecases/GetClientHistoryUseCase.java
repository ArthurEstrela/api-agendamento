package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class GetClientHistoryUseCase {

    private final IAppointmentRepository appointmentRepository;

    public Response execute(UUID clientId, int page, int size) {
        // 1. Busca histórico paginado
        PagedResult<Appointment> pagedHistory = appointmentRepository.findAllByClientId(clientId, page, size);
        List<Appointment> items = pagedHistory.items();

        // 2. Filtra agendamentos concluídos para métricas
        List<Appointment> completed = items.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();

        // 3. Calcula Ticket Médio (da página atual)
        BigDecimal totalSpent = completed.stream()
                .map(Appointment::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageTicket = completed.isEmpty() ? BigDecimal.ZERO : 
                totalSpent.divide(BigDecimal.valueOf(completed.size()), 2, RoundingMode.HALF_UP);

        // 4. Ranking de Serviços (Top 5)
        List<ItemCount> topServices = completed.stream()
                .flatMap(a -> a.getServices().stream())
                .collect(Collectors.groupingBy(Service::getName, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ItemCount(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(ItemCount::count).reversed())
                .limit(5)
                .toList();

        // 5. Ranking de Profissionais Favoritos (Top 3)
        List<ItemCount> favoriteProfessionals = completed.stream()
                .collect(Collectors.groupingBy(Appointment::getProfessionalName, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ItemCount(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(ItemCount::count).reversed())
                .limit(3)
                .toList();

        return new Response(pagedHistory, averageTicket, topServices, favoriteProfessionals);
    }

    public record Response(
            PagedResult<Appointment> history,
            BigDecimal averageTicket,
            List<ItemCount> topServices,
            List<ItemCount> favoriteProfessionals) {}

    public record ItemCount(String name, int count) {}
}