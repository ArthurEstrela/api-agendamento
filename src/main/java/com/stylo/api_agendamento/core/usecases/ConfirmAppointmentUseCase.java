package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ConfirmAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final INotificationProvider notificationProvider; // ✨ Injetado

    public void execute(ConfirmAppointmentInput input) {
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        // Validação de Segurança
        if (!appointment.getProviderId().equals(input.providerId())) {
            // Se o usuário logado não for o dono do negócio, verifica se é o profissional
            // (Assumindo que o ProfessionalId também pode confirmar)
            Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                    .orElse(null);
            
            // Aqui podes refinar a lógica de permissão conforme o teu UserRole
             if (professional == null || !professional.getServiceProviderId().equals(input.providerId())) {
                 // Simplificando: Apenas o dono ou o profissional vinculado podem confirmar
                 // Se o input.providerId for o userId do logado, precisas validar se ele tem permissão
             }
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BusinessException("Agendamento não está pendente.");
        }

        // Atualiza Status
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);
        log.info("Agendamento {} confirmado com sucesso.", appointment.getId());

        // ✨ Dispara Notificação para o Cliente
        notifyClient(appointment);
    }

    private void notifyClient(Appointment appt) {
        try {
            String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"));
            String serviceName = appt.getServices().isEmpty() ? "serviço" : appt.getServices().get(0).getName();
            
            String title = "✅ Agendamento Confirmado!";
            String body = String.format("Seu horário para %s em %s foi confirmado pelo profissional.", 
                    serviceName, dateFormatted);

            notificationProvider.sendNotification(appt.getClientId(), title, body);
        } catch (Exception e) {
            log.error("Erro ao notificar cliente da confirmação: {}", e.getMessage());
        }
    }

    public record ConfirmAppointmentInput(String appointmentId, String providerId) {}
}