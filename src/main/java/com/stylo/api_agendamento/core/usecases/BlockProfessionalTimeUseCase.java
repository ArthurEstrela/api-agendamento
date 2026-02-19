package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IEventPublisher;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class BlockProfessionalTimeUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;
    private final IServiceProviderRepository serviceProviderRepository;
    private final IEventPublisher eventPublisher;

    @Transactional
    public void execute(Input input) {
        // 1. Busca o profissional e valida existência
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 2. Busca o estabelecimento para validar regras de negócio e fuso horário
        ServiceProvider provider = serviceProviderRepository.findById(professional.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        if (!provider.isSubscriptionActive()) {
            throw new BusinessException("O estabelecimento não pode realizar operações pois a assinatura está inativa.");
        }

        // 3. Busca ocupações existentes para o dia solicitado
        // Importante para a regra de negócio de não permitir bloqueio sobre agendamentos de clientes
        List<Appointment> existingAppointments = appointmentRepository.findAllByProfessionalIdAndDate(
                professional.getId(), 
                input.start().toLocalDate()
        );

        // 4. Validação de Domínio (Encapsulada na Entidade Professional)
        // Garante que o profissional não bloqueie um horário que JÁ possua agendamentos SCHEDULED ou PENDING
        professional.validateCanBlockTime(input.start(), input.end(), existingAppointments);

        // 5. Criação do Bloqueio Pessoal usando o Factory Method do Domínio
        // O agendamento nasce com status BLOCKED e isPersonalBlock = true
        Appointment block = Appointment.createPersonalBlock(
                professional.getId(),
                professional.getName(),
                professional.getServiceProviderId(),
                input.start(),
                input.end(),
                input.reason(),
                provider.getTimeZone() // Garante o fuso horário correto para integrações externas
        );

        // 6. Persistência no Repositório de Agendamentos
        Appointment savedBlock = appointmentRepository.save(block);
        log.info("Bloqueio de agenda criado com sucesso. ID: {} | Profissional: {}", 
                savedBlock.getId(), professional.getName());

        // 7. Publicação de Evento para Sincronização (ex: Google Calendar)
        // O consumidor deste evento criará um evento externo identificado como bloqueio
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedBlock.getId(),
                professional.getId(),
                "BLOQUEIO: " + input.reason(),
                savedBlock.getStartTime()
        ));
    }

    /**
     * Input Record com UUIDs padronizados
     */
    public record Input(
            UUID professionalId,
            LocalDateTime start,
            LocalDateTime end,
            String reason
    ) {}
}