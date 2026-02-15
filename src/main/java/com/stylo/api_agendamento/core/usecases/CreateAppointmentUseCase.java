package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.ScheduleConflictException;
import com.stylo.api_agendamento.core.ports.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;
    private final IUserRepository userRepository;
    
    // ✨ ADICIONADO: Precisamos buscar o dono do salão para saber o Fuso Horário
    private final IServiceProviderRepository serviceProviderRepository;

    private final IEventPublisher eventPublisher;

    @Transactional
    public Appointment execute(CreateAppointmentInput input) {
        
        // 1. Busca Profissional
        Professional professional = professionalRepository.findByIdWithLock(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Validações de Cliente
        User client = userRepository.findById(input.clientId())
                .orElseThrow(() -> new BusinessException("Cliente não encontrado."));

        // 3. Validação de Serviços
        List<Service> requestedServices = serviceRepository.findAllByIds(input.serviceIds());
        if (requestedServices.isEmpty()) {
            throw new BusinessException("Selecione ao menos um serviço.");
        }

        // 4. Validação de Disponibilidade
        professional.validateCanPerform(requestedServices);

        int totalDuration = requestedServices.stream().mapToInt(Service::getDuration).sum();
        if (!professional.isAvailable(input.startTime(), totalDuration)) {
            throw new BusinessException("Profissional indisponível neste horário (fora do expediente ou pausa).");
        }

        // 5. Verificação de Conflito
        boolean hasConflict = appointmentRepository.hasConflictingAppointment(
                input.professionalId(),
                input.startTime(),
                input.startTime().plusMinutes(totalDuration));

        if (hasConflict) {
            throw new ScheduleConflictException("Este horário acabou de ser ocupado por outro cliente.");
        }

        // 6. Recuperação do TimeZone (CORRIGIDO)
        // Buscamos o ServiceProvider usando o ID que está no Profissional
        String timeZone = serviceProviderRepository.findById(professional.getServiceProviderId())
                .map(ServiceProvider::getTimeZone)
                .orElse("America/Sao_Paulo"); // Fallback se não encontrar (raro)

        // 7. Criação do Objeto de Domínio
        Appointment appointment = Appointment.create(
                client.getId(),
                client.getName(),
                client.getEmail(),
                professional.getServiceProviderName(),
                new ClientPhone(client.getPhoneNumber()),
                professional.getServiceProviderId(),
                professional.getId(),
                professional.getName(),
                requestedServices,
                input.startTime(),
                input.reminderMinutes(),
                timeZone // ✨ Passando o TimeZone recuperado
        );

        // 8. Persistência
        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento criado (ID: {}). TimeZone: {}", savedAppointment.getId(), timeZone);

        // 9. Publicação do Evento Assíncrono
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedAppointment.getId(),
                professional.getId(),
                client.getName(),
                savedAppointment.getStartTime()
        ));

        return savedAppointment;
    }

    public record CreateAppointmentInput(
            String clientId,
            String professionalId,
            List<String> serviceIds,
            LocalDateTime startTime,
            Integer reminderMinutes) {
    }
}