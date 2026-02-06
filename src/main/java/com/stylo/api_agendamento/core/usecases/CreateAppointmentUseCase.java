package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IUserRepository userRepository; // Para buscar dados do cliente

    public Appointment execute(CreateAppointmentInput input) {
        // 1. Buscar o Profissional e validar existência
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Buscar o Cliente (Usuário) para obter nome e telefone
        User client = userRepository.findById(input.clientId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));

        // 3. Buscar e validar os serviços solicitados
        List<Service> requestedServices = serviceRepository.findAllByIds(input.serviceIds());
        if (requestedServices.isEmpty()) {
            throw new BusinessException("É necessário selecionar ao menos um serviço válido.");
        }

        // 4. Regra de Negócio: Validar se o profissional realiza esses serviços
        professional.validateCanPerform(requestedServices);

        // 5. Regra de Negócio: Validar se o horário está dentro da grade (isAvailable)
        int totalDuration = requestedServices.stream().mapToInt(Service::getDuration).sum();
        if (!professional.isAvailable(input.startTime(), totalDuration)) {
            throw new BusinessException("O profissional não está disponível ou está fechado neste horário.");
        }

        // 6. Criar a instância do Domínio (Usa a fábrica blindada)
        // O método 'create' já calcula o endTime e o totalPrice internamente
        Appointment appointment = Appointment.create(
                client.getId(),
                client.getName(),
                new ClientPhone(client.getPhoneNumber()),
                professional.getServiceProviderId(),
                professional.getId(),
                professional.getName(),
                requestedServices,
                input.startTime()
        );

        // 7. Proteção Anti-Conflito (Double Booking)
        // Verificamos no banco se alguém agendou o mesmo intervalo enquanto o cliente escolhia
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                appointment.getProfessionalId(),
                appointment.getStartTime(),
                appointment.getEndTime()
        );

        if (hasConflict) {
            throw new BusinessException("Este horário acabou de ser ocupado. Por favor, escolha outro.");
        }

        // 8. Persistência
        return appointmentRepository.save(appointment);
    }

    // DTO de entrada para manter o Use Case puro
    public record CreateAppointmentInput(
            String clientId,
            String professionalId,
            List<String> serviceIds,
            LocalDateTime startTime
    ) {}
}