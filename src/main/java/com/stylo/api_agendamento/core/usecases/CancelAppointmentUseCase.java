package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IUserRepository userRepository; // Necessário para achar o userId do profissional
    private final INotificationProvider notificationProvider; // ✨ Injetado

    public void execute(CancelAppointmentInput input) {
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Agendamento já está cancelado.");
        }

        // Validação de Prazo (Exemplo: 2 horas antes)
        // Podes trazer a regra de cancelamento do ServiceProvider aqui se quiseres ser estrito
        if (input.isClient() && appointment.getStartTime().isBefore(LocalDateTime.now().plusHours(2))) {
             // throw new BusinessException("Cancelamento permitido apenas com 2h de antecedência.");
             // (Comentado para manter flexível por enquanto)
        }

        // Atualiza Status
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(input.reason());
        appointment.setCancelledBy(input.userId());
        appointmentRepository.save(appointment);
        
        log.info("Agendamento {} cancelado por {}.", appointment.getId(), input.userId());

        // ✨ Lógica de Notificação Cruzada
        notifyOtherParty(appointment, input.userId());
    }

    private void notifyOtherParty(Appointment appt, String cancelledById) {
        try {
            String dateFormatted = appt.getStartTime().format(DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"));
            String serviceName = appt.getServices().isEmpty() ? "atendimento" : appt.getServices().get(0).getName();
            boolean isClientCancelled = cancelledById.equals(appt.getClientId());

            if (isClientCancelled) {
                // ➡️ Cliente Cancelou: Avisa Profissional + Dono
                String title = "⚠️ Agendamento Cancelado";
                String body = String.format("%s cancelou o agendamento de %s em %s.", 
                        appt.getClientName(), serviceName, dateFormatted);

                Set<String> recipients = new HashSet<>();
                recipients.add(appt.getServiceProviderId()); // Dono
                
                // Busca ID do usuário do profissional
                userRepository.findByProfessionalId(appt.getProfessionalId())
                        .ifPresent(u -> recipients.add(u.getId()));

                for (String recipientId : recipients) {
                    notificationProvider.sendNotification(recipientId, title, body);
                }
            } else {
                // ➡️ Profissional/Dono Cancelou: Avisa Cliente
                String title = "❌ Agendamento Cancelado";
                String body = String.format("Seu agendamento de %s em %s foi cancelado pelo estabelecimento.", 
                        serviceName, dateFormatted);
                
                notificationProvider.sendNotification(appt.getClientId(), title, body);
            }

        } catch (Exception e) {
            log.error("Erro ao enviar notificação de cancelamento: {}", e.getMessage());
        }
    }

    public record CancelAppointmentInput(String appointmentId, String userId, String reason, boolean isClient) {}
}