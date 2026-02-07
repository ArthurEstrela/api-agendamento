package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
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

        // 2. Busca ocupações do dia para verificar conflitos
        List<Appointment> currentOccupations = appointmentRepository.findAllByProfessionalIdAndDate(
                professional.getId(), 
                input.start().toLocalDate()
        );

        // 3. Validação de Domínio: Garante que o profissional não bloqueie um horário com cliente
        professional.validateCanBlockTime(input.start(), input.end(), currentOccupations);

        // 4. Criação usando a Fábrica de Domínio (Centraliza a lógica de "O que é um bloqueio")
        // Isso já seta isPersonalBlock = true e FinalPrice = 0 automaticamente
        Appointment block = Appointment.createPersonalBlock(
                professional.getId(),
                professional.getName(),
                professional.getServiceProviderId(),
                input.start(),
                input.end(),
                input.reason()
        );

        // 5. Persistência
        appointmentRepository.save(block);
    }

    public record BlockTimeInput(
            String professionalId,
            LocalDateTime start,
            LocalDateTime end,
            String reason
    ) {}
}