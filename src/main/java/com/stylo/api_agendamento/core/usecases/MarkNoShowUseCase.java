package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IClientRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class MarkNoShowUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IClientRepository clientRepository; // ✨ Repositório de Clientes

    @Transactional
    public void execute(String appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        // Regra de Negócio: Só pode marcar No-Show se estiver agendado
        appointment.markAsNoShow();
        appointmentRepository.save(appointment);

        // ✨ Atualiza a reputação do cliente
        // Assumindo que você tem um método para buscar o Cliente pelo userId e
        // providerId
        // ou que o Appointment tem o ID do "Client" (tabela de relacionamento) e não só
        // do User.
        // Se appointment.getClientId() for o ID da tabela 'clients', perfeito.
        clientRepository.findById(appointment.getClientId()).ifPresent(client -> {
            client.incrementNoShow();
            clientRepository.save(client);
        });
    }
}