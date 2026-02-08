package com.stylo.api_agendamento.adapters.outbound.notifications;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions; // Import correto do SDK 3.1.0
import com.stylo.api_agendamento.core.ports.INotificationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResendNotificationAdapter implements INotificationProvider {

    private final Resend resend;
    private final String fromEmail;

    public ResendNotificationAdapter(
            @Value("${resend.api.key}") String apiKey,
            @Value("${resend.from.email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendAppointmentReminder(String to, String clientName, String businessName, String startTime) {
        String subject = "Lembrete: Seu horário na " + businessName;
        String content = buildHtml(clientName, "Este é um lembrete amigável do seu agendamento.", businessName, startTime);
        sendEmail(to, subject, content);
    }

    @Override
    public void sendAppointmentConfirmed(String to, String message) {
        // Implementação para cumprir o contrato da interface
        sendEmail(to, "Agendamento Confirmado", buildSimpleHtml(message));
    }

    @Override
    public void sendAppointmentRescheduled(String to, String message) {
        sendEmail(to, "Agendamento Reagendado", buildSimpleHtml(message));
    }

    @Override
    public void sendAppointmentCancelled(String to, String message) {
        sendEmail(to, "Agendamento Cancelado", buildSimpleHtml(message));
    }

    /**
     * Método privado centralizador de envio para evitar repetição de código
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            resend.emails().send(options);
            log.info("E-mail '{}' enviado com sucesso para: {}", subject, to);

        } catch (ResendException e) {
            log.error("Falha ao enviar e-mail via Resend para {}: {}", to, e.getMessage());
        }
    }

    private String buildHtml(String clientName, String bodyText, String businessName, String startTime) {
        return """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #eee; border-radius: 10px; padding: 20px;">
                <h2 style="color: #333; text-align: center;">Olá, %s! 👋</h2>
                <p style="font-size: 16px; color: #555; line-height: 1.5;">%s</p>
                <div style="background-color: #f9f9f9; padding: 15px; border-radius: 8px; text-align: center; margin: 20px 0;">
                    <span style="display: block; font-size: 14px; color: #888;">ESTABELECIMENTO: %s</span>
                    <span style="font-size: 24px; font-weight: bold; color: #000;">%s</span>
                </div>
                <p style="text-align: center; font-size: 12px; color: #aaa;">Powered by Stylo</p>
            </div>
            """.formatted(clientName, bodyText, businessName, startTime);
    }

    private String buildSimpleHtml(String message) {
        return "<div style='font-family: sans-serif; padding: 20px;'>" + message + "</div>";
    }
}