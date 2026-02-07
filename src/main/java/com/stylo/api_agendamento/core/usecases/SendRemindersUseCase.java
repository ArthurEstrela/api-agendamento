package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class SendRemindersUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final INotificationProvider notificationProvider;

    public void execute() {
        LocalDateTime now = LocalDateTime.now();

        // Busca agendamentos onde: (startTime - reminderMinutes) <= agora
        // E que ainda não foram notificados
        List<Appointment> toNotify = appointmentRepository.findAppointmentsToNotify(now);

        for (Appointment app : toNotify) {
            // Só envia se houver um clientId (cliente com cadastro no app)
            if (app.getClientId() != null) {
                String message = String.format(
                        "Olá %s, lembrete do seu agendamento com %s às %s!",
                        app.getClientName(),
                        app.getProfessionalName(),
                        app.getStartTime().toLocalTime());

                notificationProvider.sendAppointmentReminder(app.getClientId(), message);
            }

            // Mesmo se for manual, marcamos como "notificado" para o Worker não pegá-lo de
            // novo
            app.markAsNotified();
            appointmentRepository.save(app);
        }
    }
}