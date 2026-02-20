package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IClientRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class MarkNoShowUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IClientRepository clientRepository;
    private final IServiceProviderRepository providerRepository;

    @Transactional
    public void execute(UUID appointmentId) {
        // 1. Busca e Valida o Agendamento
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus().isTerminalState()) {
            throw new BusinessException("Não é possível marcar falta em um agendamento já finalizado ou cancelado.");
        }

        // 2. Atualiza o Agendamento no Domínio
        appointment.markAsNoShow();
        appointmentRepository.save(appointment);

        // 3. Atualiza a Reputação do Cliente (Incremental)
        clientRepository.findById(appointment.getClientId()).ifPresent(client -> {
            client.incrementNoShow();
            clientRepository.save(client);
            
            log.warn("Falta registrada para o cliente {}. Total de faltas agora: {}", 
                    client.getName(), client.getNoShowCount());

            // 4. Verificação de Limite de Bloqueio (Lógica de Prevenção)
            checkBlockingThreshold(client.getNoShowCount(), appointment.getServiceProviderId());
        });
    }

    private void checkBlockingThreshold(int currentNoShows, UUID providerId) {
        providerRepository.findById(providerId).ifPresent(provider -> {
            if (currentNoShows >= provider.getMaxNoShowsAllowed()) {
                log.error("ALERTA: Cliente atingiu o limite de {} faltas no estabelecimento {}.", 
                        provider.getMaxNoShowsAllowed(), provider.getBusinessName());
                // Futuramente: disparar notificação para o gerente ou bloquear agendamentos online
            }
        });
    }
}