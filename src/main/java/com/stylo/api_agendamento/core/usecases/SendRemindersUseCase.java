package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Recomendado para produção
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SendRemindersUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final INotificationProvider notificationProvider;

    public void execute() {
        LocalDateTime now = LocalDateTime.now();
        
        // Busca agendamentos CONFIRMADOS que ainda não enviaram lembrete
        List<Appointment> appointments = appointmentRepository.findPendingReminders(now);

        for (Appointment appt : appointments) {
            try {
                notificationProvider.sendAppointmentReminder(
                        appt.getClientEmail(),
                        appt.getClientName(),
                        appt.getBusinessName(),
                        appt.getStartTime().toString()
                );

                appt.markReminderAsSent();
                appointmentRepository.save(appt);
                
                log.info("Lembrete enviado com sucesso para: {}", appt.getClientEmail());
            } catch (Exception e) {
                log.error("Erro ao processar lembrete do agendamento {}: {}", appt.getId(), e.getMessage());
            }
        }
    }
}