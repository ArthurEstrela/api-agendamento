package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MarkNoShowUseCase {
    private final IAppointmentRepository appointmentRepository;

    public void execute(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        appointment.markAsNoShow();
        appointmentRepository.save(appointment);
        
        // Log para auditoria de faltas do cliente
        log.info("Cliente {} marcado como No-Show no agendamento {}", 
            appointment.getClientName(), appointment.getId());
    }
}