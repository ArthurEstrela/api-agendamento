package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Motor de notificações agendadas. 
 * Este UseCase deve ser chamado por um Scheduler (ex: a cada 5 ou 10 minutos).
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
public class SendPendingRemindersUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final INotificationProvider notificationProvider;
    private final IUserRepository userRepository;

    @Transactional
    public void execute() {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. Busca agendamentos SCHEDULED onde (startTime - reminderMinutes) <= agora
        // E que ainda não tiveram o lembrete enviado (reminderSent = false)
        List<Appointment> pendingAppointments = appointmentRepository.findPendingReminders(now);

        if (pendingAppointments.isEmpty()) {
            return;
        }

        log.info("⏰ Iniciando processamento de {} lembretes de agendamento.", pendingAppointments.size());

        for (Appointment appt : pendingAppointments) {
            try {
                // 2. Orquestração de Canais (E-mail e Push)
                dispatchMultiChannelNotification(appt);

                // 3. Atualização de Estado (Idempotência)
                // Marcamos no domínio que o lembrete foi processado com sucesso
                appt.markReminderAsSent();
                appointmentRepository.save(appt);
                
                log.info("✅ Lembrete entregue para: {} (Agendamento: {})", appt.getClientName(), appt.getId());

            } catch (Exception e) {
                log.error("❌ Falha crítica ao processar lembrete {}: {}", appt.getId(), e.getMessage());
                // Não relançamos para não travar a fila de outros agendamentos
            }
        }
    }

    /**
     * Gerencia a entrega da mensagem para os provedores externos.
     */
    private void dispatchMultiChannelNotification(Appointment appt) {
        // Formatação amigável baseada no contexto do agendamento
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        String timeFormatted = appt.getStartTime().format(timeFormatter);
        
        // Lista os serviços para dar clareza ao cliente
        String servicesList = appt.getServices().stream()
                .map(s -> s.getName())
                .collect(Collectors.joining(", "));

        String title = "🔔 Lembrete Stylo";
        String body = String.format("Olá %s! Passando para lembrar seu horário hoje às %s para [%s] no %s.", 
                appt.getClientName(), timeFormatted, servicesList, appt.getBusinessName());

        // --- CANAL A: E-mail ---
        try {
            notificationProvider.sendAppointmentReminder(
                    appt.getClientEmail(),
                    appt.getClientName(),
                    appt.getBusinessName(),
                    timeFormatted,
                    servicesList
            );
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail para {}: {}", appt.getClientEmail(), e.getMessage());
        }

        // --- CANAL B: Push Notification (Via FCM) ---
        // Buscamos o usuário para garantir que temos o token de push atualizado
        if (appt.getClientId() != null) {
            userRepository.findById(appt.getClientId()).ifPresent(user -> {
                if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
                    notificationProvider.sendPushNotification(
                            user.getId(), 
                            title, 
                            body, 
                            "/client/appointments/" + appt.getId()
                    );
                }
            });
        }
    }
}