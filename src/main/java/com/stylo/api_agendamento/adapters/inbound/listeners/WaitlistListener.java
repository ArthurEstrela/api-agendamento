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
        log.info("Processando lista de espera para cancelamento do agendamento: {}", event.appointmentId());

        // 1. Busca quem está na fila (✨ Nome do método corrigido conforme IWaitlistRepository)
        List<Waitlist> waitingClients = waitlistRepository.findAllByProfessionalIdAndDate(
                event.professionalId(),
                event.startTime().toLocalDate()
        );

        if (waitingClients.isEmpty()) {
            log.debug("Nenhum cliente aguardando para este profissional no dia {}", event.startTime().toLocalDate());
            return;
        }

        String timeFormatted = event.startTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String title = "🎉 Vaga Disponível!";
        String body = String.format("Uma vaga abriu hoje às %s com o profissional que você desejava! Agende agora.", timeFormatted);

        // 2. Notifica os clientes da fila
        for (Waitlist client : waitingClients) {
            // ✨ Melhoria: Pula clientes que já foram notificados para este dia
            if (client.isNotified()) continue;

            try {
                // ✨ Melhoria: Verifica se o cliente tem ID para Push (clientId no Waitlist é opcional)
                if (client.getClientId() != null) {
                    // ✨ Nome do método corrigido conforme INotificationProvider
                    notificationProvider.sendPushNotification(
                            client.getClientId(),
                            title,
                            body,
                            "/booking/" + event.professionalId()
                    );
                } else if (client.getClientEmail() != null) {
                    // ✨ Fallback: Se não tem App/ID, poderia enviar um e-mail (opcional)
                    log.info("Cliente {} não tem ID de usuário, e-mail seria enviado para {}", client.getClientName(), client.getClientEmail());
                }

                // 3. Atualiza o estado da fila para evitar SPAM
                client.markAsNotified();
                waitlistRepository.save(client);
                
                log.info("Notificação enviada para o cliente da fila: {}", client.getClientName());

            } catch (Exception e) {
                log.error("Erro ao notificar cliente {} da lista de espera", client.getClientId(), e);
            }
        }
    }
}