package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.ICalendarProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IServiceProviderRepository serviceProviderRepository;
    private final ICalendarProvider calendarProvider;

    public void execute(String appointmentId) {
        // 1. Busca Agendamento
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Busca Estabelecimento (para regras de cancelamento)
        ServiceProvider provider = serviceProviderRepository.findById(appointment.getProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 3. Valida Política de Cancelamento (ex: antecedência mínima)
        provider.validateCancellationPolicy(appointment.getStartTime());

        // 4. Realiza Cancelamento no Core
        appointment.cancel();
        appointmentRepository.save(appointment); // Salva status CANCELLED

        log.info("Agendamento {} cancelado no Stylo.", appointmentId);

        // 5. Remove do Google Calendar (Se houver vínculo)
        // Agora o getExternalEventId() vai funcionar porque adicionamos no passo 1
        if (appointment.getExternalEventId() != null && !appointment.getExternalEventId().isBlank()) {
            try {
                calendarProvider.deleteEvent(
                        appointment.getProfessionalId(),
                        appointment.getExternalEventId());
            } catch (Exception e) {
                log.warn("Falha ao remover do Google (pode já ter sido removido): {}", e.getMessage());
            }
        }
    }
}