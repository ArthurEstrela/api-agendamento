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
        // 1. Busca o profissional e valida existência
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Busca agendamentos no dia para verificar conflitos antes de bloquear
        // Usamos a data do início do bloqueio para filtrar a busca no repositório
        List<Appointment> currentAppointments = appointmentRepository.findAllByProfessionalIdAndDate(
                professional.getId(), 
                input.start().toLocalDate()
        );

        // 3. Valida no domínio se o bloqueio conflita com agendamentos SCHEDULED ou PENDING
        professional.validateCanBlockTime(input.start(), input.end(), currentAppointments);

        // 4. Criação do registro de bloqueio usando o novo status BLOCKED
        // Importante preencher professionalName e providerId para consistência dos dados
        Appointment block = Appointment.builder()
                .professionalId(professional.getId())
                .professionalName(professional.getName())
                .providerId(professional.getServiceProviderId())
                .startTime(input.start())
                .endTime(input.end())
                .status(AppointmentStatus.BLOCKED) // Usando o status específico para administração
                .notes("BLOQUEIO ADMINISTRATIVO: " + input.reason())
                .createdAt(LocalDateTime.now())
                .build();

        // 5. Persistência do bloqueio como uma "ocupação" na agenda
        appointmentRepository.save(block);
    }

    public record BlockTimeInput(
            String professionalId,
            LocalDateTime start,
            LocalDateTime end,
            String reason
    ) {}
}