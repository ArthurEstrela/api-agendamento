package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@UseCase
@RequiredArgsConstructor
public class CreateWalkInUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IServiceProviderRepository serviceProviderRepository;
    private final IClientRepository clientRepository; // Necessário buscar ou criar cliente rápido

    public Appointment execute(WalkInInput input) {
        // 1. Validar Profissional
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Identificar ou Criar Cliente Temporário
        String clientId = null;
        String clientName = input.clientName();
        ClientPhone phone = null;

        if (input.clientId() != null) {
            // Cliente Cadastrado
            var client = clientRepository.findById(input.clientId())
                    .orElseThrow(() -> new BusinessException("Cliente não encontrado."));
            clientId = client.getId();
            clientName = client.getName();
            phone = client.getPhoneNumber();
        } else {
            // Cliente Avulso/Anônimo
            if (clientName == null || clientName.isBlank())
                clientName = "Cliente Balcão";
            // Em walk-in, telefone pode ser opcional se for cliente anônimo
            phone = input.clientPhone() != null ? new ClientPhone(input.clientPhone()) : new ClientPhone("00000000000");
        }

        // 3. Buscar Serviços (se houver - pode ser uma venda só de produto)
        List<Service> services = List.of();
        if (input.serviceIds() != null && !input.serviceIds().isEmpty()) {
            services = serviceRepository.findAllByIds(input.serviceIds());
            professional.validateCanPerform(services);
        }

        // 4. Definir Horário (Agora)
        LocalDateTime startTime = LocalDateTime.now();
        String timeZone = serviceProviderRepository.findById(professional.getServiceProviderId())
                .map(ServiceProvider::getTimeZone)
                .orElse("America/Sao_Paulo");

        // 5. Criar Agendamento com Status ANDAMENTO (In Progress) ou CONFIRMADO
        // Usamos o createManual, mas forçamos o status se necessário
        Appointment appointment = Appointment.createManual(
                clientName,
                phone,
                professional.getServiceProviderId(),
                professional.getId(),
                professional.getName(),
                services,
                startTime,
                "Walk-in / Encaixe Rápido",
                timeZone);

        if (clientId != null) {
            // Se tiver ID, associamos. O createManual original não pedia ID, então usamos o
            // Builder
            appointment = appointment.toBuilder().clientId(clientId).build();
        }

        // Já confirma automaticamente pois o cliente está na loja
        appointment.confirm();

        return appointmentRepository.save(appointment);
    }

    public record WalkInInput(
            String professionalId,
            String clientId, // Opcional
            String clientName, // Usado se clientId for null
            String clientPhone, // Opcional
            List<String> serviceIds) {
    }
}