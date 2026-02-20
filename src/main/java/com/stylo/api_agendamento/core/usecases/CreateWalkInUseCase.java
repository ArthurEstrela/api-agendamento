package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateWalkInUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IServiceProviderRepository serviceProviderRepository;
    private final IClientRepository clientRepository;

    @Transactional
    public Appointment execute(Input input) {
        // 1. Validar Profissional e Contexto
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        ServiceProvider provider = serviceProviderRepository.findById(professional.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 2. Identificar ou Resolver Dados do Cliente
        UUID clientId = null;
        String clientName = input.clientName();
        ClientPhone phone;

        if (input.clientId() != null) {
            Client client = clientRepository.findById(input.clientId())
                    .orElseThrow(() -> new EntityNotFoundException("Cliente cadastrado não encontrado."));
            clientId = client.getId();
            clientName = client.getName();
            phone = client.getPhoneNumber();
        } else {
            // Cliente Balcão/Anônimo
            if (clientName == null || clientName.isBlank()) clientName = "Cliente Balcão";
            phone = input.clientPhone() != null ? new ClientPhone(input.clientPhone()) : new ClientPhone("00000000000");
        }

        // 3. Buscar Serviços (Valida competência do profissional)
        List<Service> services = List.of();
        if (input.serviceIds() != null && !input.serviceIds().isEmpty()) {
            services = serviceRepository.findAllByIds(input.serviceIds());
            professional.validateCanPerform(services);
        }

        // 4. Criação do Agendamento Presencial (Status imediato SCHEDULED)
        Appointment appointment = Appointment.createManual(
                clientName,
                phone,
                provider.getId(),
                professional.getId(),
                professional.getName(),
                services,
                LocalDateTime.now(), // Início imediato
                "Atendimento Presencial (Walk-in)",
                provider.getTimeZone()
        );

        if (clientId != null) {
            appointment = appointment.toBuilder().clientId(clientId).build();
        }

        // Confirma automaticamente pois o cliente já está na cadeira
        appointment.confirm();

        log.info("Walk-in registrado: {} para o cliente {}", appointment.getId(), clientName);
        return appointmentRepository.save(appointment);
    }

    public record Input(
            UUID professionalId,
            UUID clientId,
            String clientName,
            String clientPhone,
            List<UUID> serviceIds
    ) {}
}