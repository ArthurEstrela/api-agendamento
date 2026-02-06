package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class BlockProfessionalTimeUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;

    public void execute(BlockTimeInput input) {
        // 1. Busca o profissional
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Busca agendamentos no período para verificar conflitos antes de bloquear
        List<Appointment> conflicts = appointmentRepository.findAllByProfessionalIdAndDate(
                professional.getId(), 
                input.start().toLocalDate()
        );

        // 3. Valida no domínio se o bloqueio é permitido
        professional.validateCanBlockTime(input.start(), input.end(), conflicts);

        // 4. Cria um agendamento especial de sistema (Status: CANCELLED ou um novo status BLOCK)
        // DICA: Você pode usar o AppointmentStatus.CANCELLED ou criar o status BLOCKED no seu enum
        Appointment block = Appointment.builder()
                .professionalId(professional.getId())
                .providerId(professional.getServiceProviderId())
                .startTime(input.start())
                .status(AppointmentStatus.CANCELLED) // Ou adicione BLOCKED ao seu AppointmentStatus.java
                .notes("BLOQUEIO DE AGENDA: " + input.reason())
                .build();

        appointmentRepository.save(block);
    }

    public record BlockTimeInput(
            String professionalId,
            LocalDateTime start,
            LocalDateTime end,
            String reason
    ) {}
}