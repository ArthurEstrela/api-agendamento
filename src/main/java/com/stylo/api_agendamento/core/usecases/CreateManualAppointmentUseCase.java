package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class CreateManualAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;

    public Appointment execute(ManualInput input) {
        // 1. Busca o Profissional
        Professional professional = professionalRepository.findById(input.professionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Busca e valida os serviços
        List<Service> services = serviceRepository.findAllByIds(input.serviceIds());
        if (services.isEmpty()) throw new BusinessException("Selecione ao menos um serviço.");

        // 3. Validação de conflitos e grade
        int duration = services.stream().mapToInt(Service::getDuration).sum();
        if (!professional.isAvailable(input.startTime(), duration)) {
            throw new BusinessException("Horário fora da grade do profissional.");
        }

        // 4. Criação usando a nova fábrica de Walk-in
        Appointment appointment = Appointment.createManual(
                input.clientName(),
                new ClientPhone(input.clientPhone()),
                professional.getServiceProviderId(),
                professional.getId(),
                professional.getName(),
                services,
                input.startTime(),
                input.notes()
        );

        // 5. Proteção de Conflito Atômica
        if (appointmentRepository.hasConflictingAppointment(
                appointment.getProfessionalId(), appointment.getStartTime(), appointment.getEndTime())) {
            throw new BusinessException("Conflito de horário detectado.");
        }

        return appointmentRepository.save(appointment);
    }

    public record ManualInput(
            String professionalId,
            String clientName,
            String clientPhone,
            List<String> serviceIds,
            LocalDateTime startTime,
            String notes
    ) {}
}