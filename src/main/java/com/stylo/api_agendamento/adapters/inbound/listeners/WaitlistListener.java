package com.stylo.api_agendamento.adapters.inbound.listeners;

import com.stylo.api_agendamento.core.domain.Waitlist;
import com.stylo.api_agendamento.core.domain.events.AppointmentCancelledEvent;
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import com.stylo.api_agendamento.core.ports.IWaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WaitlistListener {

    private final IWaitlistRepository waitlistRepository;
    private final INotificationProvider notificationProvider;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCancellation(AppointmentCancelledEvent event) {
        log.info("Verificando lista de espera para o cancelamento: {}", event.appointmentId());

        // 1. Busca quem está esperando por este profissional NESTE DIA
        List<Waitlist> waitingClients = waitlistRepository.findAllByProfessionalAndDate(
                event.professionalId(),
                event.startTime().toLocalDate()
        );

        if (waitingClients.isEmpty()) {
            return;
        }

        String timeFormatted = event.startTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String title = "🎉 Vaga Disponível!";
        String body = "Um horário acabou de vagar para hoje às " + timeFormatted + "! Toque para agendar antes que acabe.";

        // 2. Notifica todos (First come, first served)
        for (Waitlist client : waitingClients) {
            try {
                // Envia Push Notification
                notificationProvider.sendNotification(
                        client.getClientId(),
                        title,
                        body,
                        "/booking/" + event.professionalId() // Deep link para a tela de agendamento
                );
                
                // Marca que foi avisado (para não spamar se vagar outro horário 5 min depois, opcional)
                client.markAsNotified();
                waitlistRepository.save(client);
                
            } catch (Exception e) {
                log.error("Erro ao notificar cliente da lista de espera: {}", client.getClientId(), e);
            }
        }
    }
}