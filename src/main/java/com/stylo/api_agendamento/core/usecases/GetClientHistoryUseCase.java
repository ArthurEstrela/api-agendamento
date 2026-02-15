package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class GetClientHistoryUseCase {

    private final IAppointmentRepository appointmentRepository;

    public ClientHistoryResponse execute(String clientId) {
        // 1. Busca todos os agendamentos do cliente (precisa adicionar este método no Port)
        List<Appointment> allAppointments = appointmentRepository.findAllByClientId(clientId);

        // 2. Filtra agendamentos concluídos para estatísticas
        List<Appointment> completed = allAppointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();

        // 3. Calcula Serviços mais realizados
        List<ItemCount> topServices = completed.stream()
                .flatMap(a -> a.getServices().stream())
                .collect(Collectors.groupingBy(Service::getName, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ItemCount(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(ItemCount::count).reversed())
                .limit(5)
                .toList();

        // 4. Calcula Profissionais favoritos
        List<ItemCount> favoriteProfessionals = completed.stream()
                .collect(Collectors.groupingBy(Appointment::getProfessionalName, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ItemCount(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(ItemCount::count).reversed())
                .limit(3)
                .toList();

        // 5. Separa Histórico (ordenado pelo mais recente)
        List<Appointment> history = allAppointments.stream()
                .sorted(Comparator.comparing(Appointment::getStartTime).reversed())
                .toList();

        return new ClientHistoryResponse(history, topServices, favoriteProfessionals);
    }

    public record ClientHistoryResponse(
            List<Appointment> appointments,
            List<ItemCount> topServices,
            List<ItemCount> favoriteProfessionals
    ) {}

    public record ItemCount(String name, int count) {}
}