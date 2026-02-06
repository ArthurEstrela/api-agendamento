package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IServiceProviderRepository serviceProviderRepository;

    public void execute(String appointmentId) {
        // 1. Recupera o agendamento
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Recupera o ServiceProvider para validar a política de cancelamento
        ServiceProvider provider = serviceProviderRepository.findById(appointment.getProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 3. REGRA MONSTRA: Valida se o tempo mínimo para cancelamento foi respeitado
        // Esta regra já está encapsulada no seu domínio ServiceProvider
        provider.validateCancellationPolicy(appointment.getStartTime());

        // 4. Executa a transição de estado no domínio
        appointment.cancel();

        // 5. Persiste a alteração
        appointmentRepository.save(appointment);
    }
}