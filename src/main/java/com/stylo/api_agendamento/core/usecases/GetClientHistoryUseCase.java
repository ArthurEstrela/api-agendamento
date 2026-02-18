package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import lombok.RequiredArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class GetClientHistoryUseCase {

    private final IAppointmentRepository appointmentRepository;

    public ClientHistoryResponse execute(String clientId, int page, int size) {
        // 1. Busca histórico paginado (Já vem ordenado por Data DESC do repositório)
        PagedResult<Appointment> pagedHistory = appointmentRepository.findAllByClientId(clientId, page, size);

        List<Appointment> currentPageItems = pagedHistory.items();

        // 2. Filtra agendamentos concluídos DENTRO DA PÁGINA ATUAL para estatísticas
        // OBS: Para estatísticas globais (de todo o histórico), o ideal seria criar
        // métodos 'count' específicos no Repositório (Aggregation Queries),
        // pois aqui estamos calculando apenas sobre os itens visíveis na página.
        List<Appointment> completed = currentPageItems.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();

        // 3. Calcula Serviços mais realizados (baseado na página atual)
        List<ItemCount> topServices = completed.stream()
                .flatMap(a -> a.getServices().stream())
                .collect(Collectors.groupingBy(Service::getName, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ItemCount(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(ItemCount::count).reversed())
                .limit(5)
                .toList();

        // 4. Calcula Profissionais favoritos (baseado na página atual)
        List<ItemCount> favoriteProfessionals = completed.stream()
                .collect(Collectors.groupingBy(Appointment::getProfessionalName, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new ItemCount(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(ItemCount::count).reversed())
                .limit(3)
                .toList();

        // 5. Retorna o PagedResult diretamente
        return new ClientHistoryResponse(pagedHistory, topServices, favoriteProfessionals);
    }

    public record ClientHistoryResponse(
            PagedResult<Appointment> history, // Agora retorna metadados de paginação
            List<ItemCount> topServices,
            List<ItemCount> favoriteProfessionals) {
    }

    public record ItemCount(String name, int count) {
    }
}