package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.appointment.AppointmentResponse;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.AddressRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.ServiceProviderRequest;
import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
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
    private final IServiceProviderRepository serviceProviderRepository;

    public Response execute(UUID clientId, int page, int size) {
        PagedResult<Appointment> pagedHistory = appointmentRepository.findAllByClientId(clientId, page, size);
        List<Appointment> items = pagedHistory.items();

        List<Appointment> completed = items.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .toList();

        BigDecimal totalSpent = completed.stream()
                .map(Appointment::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageTicket = completed.isEmpty() ? BigDecimal.ZERO :
                totalSpent.divide(BigDecimal.valueOf(completed.size()), 2, RoundingMode.HALF_UP);

        List<ItemCount> topServices = completed.stream()
                .flatMap(a -> a.getServices().stream())
                .collect(Collectors.groupingBy(Service::getName, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ItemCount(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(ItemCount::count).reversed())
                .limit(5)
                .toList();

        List<ItemCount> favoriteProfessionals = completed.stream()
                .collect(Collectors.groupingBy(Appointment::getProfessionalName, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ItemCount(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparing(ItemCount::count).reversed())
                .limit(3)
                .toList();

        List<AppointmentResponse> dtoItems = items.stream()
                .map(this::mapToResponseDTO)
                .toList();

        PagedResult<AppointmentResponse> pagedDtoHistory = new PagedResult<>(
                dtoItems,
                pagedHistory.page(),
                pagedHistory.size(),
                pagedHistory.totalElements(),
                pagedHistory.totalPages()
        );

        return new Response(pagedDtoHistory, averageTicket, topServices, favoriteProfessionals);
    }

    private AppointmentResponse mapToResponseDTO(Appointment a) {
        List<String> serviceNames = a.getServices().stream()
                .map(Service::getName)
                .toList();

        ServiceProviderRequest providerRequest = null;

        if (a.getServiceProviderId() != null) {
            var providerOpt = serviceProviderRepository.findById(a.getServiceProviderId());

            if (providerOpt.isPresent()) {
                var p = providerOpt.get();

                AddressRequest addressRequest = null;
                // Address é um Record, então usamos nomeDoCampo() em vez de getNomeDoCampo()
                if (p.getBusinessAddress() != null) {
                    addressRequest = new AddressRequest(
                            p.getBusinessAddress().street(),
                            p.getBusinessAddress().number(),
                            p.getBusinessAddress().neighborhood(),
                            p.getBusinessAddress().city(),
                            p.getBusinessAddress().state(),
                            p.getBusinessAddress().zipCode(),
                            p.getBusinessAddress().lat(), // Passando Latitude
                            p.getBusinessAddress().lng()  // Passando Longitude
                    );
                }

                // Convertendo Document VO para String usando toString() (ou value() se existir)
                String docString = p.getDocument() != null ? p.getDocument().toString() : "";

                providerRequest = new ServiceProviderRequest(
                        p.getBusinessName(),
                        "app@stylo.com", // Enviando um email fixo para validar o DTO sem vazar o real
                        "",
                        p.getBusinessName(),
                        docString,           // <-- Aqui está o Document como String
                        addressRequest,
                        p.getBusinessPhone() != null ? p.getBusinessPhone() : ""
                );
            }
        }

        // ✨ CORREÇÃO: Adicionados os campos novos exigidos pelo AppointmentResponse
        String phoneStr = a.getClientPhone() != null ? a.getClientPhone().toString() : null; 
        // Nota: Se o seu Value Object ClientPhone usar getNumber() ou value() ao invés de toString(), ajuste acima.

        return new AppointmentResponse(
                a.getId() != null ? a.getId().toString() : null,
                a.getServiceProviderId(),      // ✨ NOVO
                a.getProfessionalId(),         // ✨ NOVO
                a.getClientId(),               // ✨ NOVO
                a.getClientName(),
                phoneStr,                      // ✨ NOVO
                a.getProfessionalName(),
                null,                          // professionalAvatarUrl
                serviceNames,
                a.getStartTime(),
                a.getEndTime(),
                a.getFinalPrice(),
                a.getStatus() != null ? a.getStatus().name() : "PENDING",
                a.getNotes(),                  // ✨ NOVO
                providerRequest
        );
    }

    public record Response(
            PagedResult<AppointmentResponse> history,
            BigDecimal averageTicket,
            List<ItemCount> topServices,
            List<ItemCount> favoriteProfessionals) {}

    public record ItemCount(String name, int count) {}
}