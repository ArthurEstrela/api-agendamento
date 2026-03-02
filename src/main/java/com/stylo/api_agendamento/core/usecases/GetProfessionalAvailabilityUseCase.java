package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        ServiceProvider provider = providerRepository.findById(professional.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        List<Service> services = serviceRepository.findAllByIds(serviceIds);
        if (services.isEmpty()) {
            throw new BusinessException("Selecione ao menos um serviço.");
        }

        // Valida se o profissional pode realizar os serviços e calcula a duração total
        professional.validateCanPerform(services);
        int totalDuration = services.stream().mapToInt(Service::getDuration).sum();

        // 2. Busca ocupações existentes na data solicitada
        List<Appointment> occupations = appointmentRepository.findAllByProfessionalIdAndDate(professionalId, date)
                .stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDING ||
                        a.getStatus() == AppointmentStatus.SCHEDULED ||
                        a.getStatus() == AppointmentStatus.BLOCKED) // ✨ Muito mais explícito e seguro
                .toList();

        // 3. Delegar o cálculo complexo de slots para a entidade de Domínio
        List<LocalTime> availableSlots = professional.calculateAvailableSlots(
                date,
                totalDuration,
                occupations,
                ZoneId.of(provider.getTimeZone()));

        // Validação extra: se a lista voltar vazia e o profissional nem sequer atende
        // nesse dia
        if (availableSlots.isEmpty() && !professional.isAvailable(date.atStartOfDay(), totalDuration)) {
            throw new BusinessException(
                    "O profissional não atende nesta data ou não há horário suficiente para a duração dos serviços.");
        }

        return availableSlots;
    }
}