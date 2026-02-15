package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.events.AppointmentCreatedEvent;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IEventPublisher;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class BlockProfessionalTimeUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;
    // ✨ Necessário para buscar o TimeZone
    private final IServiceProviderRepository serviceProviderRepository;
    // ✨ Opcional: Se quiser que o bloqueio vá pro Google Calendar também
    private final IEventPublisher eventPublisher;

    @Transactional
    public void execute(BlockTimeInput input) {
        // 1. Busca o profissional e valida existência
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Busca ocupações do dia para verificar conflitos
        List<Appointment> currentOccupations = appointmentRepository.findAllByProfessionalIdAndDate(
                professional.getId(), 
                input.start().toLocalDate()
        );

        // 3. Validação de Domínio
        // Garante que o profissional não bloqueie um horário que JÁ tem cliente
        professional.validateCanBlockTime(input.start(), input.end(), currentOccupations);

        // 4. Recuperação do TimeZone
        String timeZone = serviceProviderRepository.findById(professional.getServiceProviderId())
                .map(ServiceProvider::getTimeZone)
                .orElse("America/Sao_Paulo");

        // 5. Criação usando a Fábrica de Domínio (Passando TimeZone)
        Appointment block = Appointment.createPersonalBlock(
                professional.getId(),
                professional.getName(),
                professional.getServiceProviderId(),
                input.start(),
                input.end(),
                input.reason(),
                timeZone // ✨ Argumento corrigido
        );

        // 6. Persistência
        Appointment savedBlock = appointmentRepository.save(block);
        log.info("Bloqueio de agenda criado: {}", savedBlock.getId());

        // 7. Sincronização (Opcional, mas recomendado para travar o Google Calendar também)
        eventPublisher.publish(new AppointmentCreatedEvent(
                savedBlock.getId(),
                professional.getId(),
                "Bloqueio: " + input.reason(), // Aparecerá assim no Google
                savedBlock.getStartTime()
        ));
    }

    public record BlockTimeInput(
            String professionalId,
            LocalDateTime start,
            LocalDateTime end,
            String reason
    ) {}
}