package com.stylo.api_agendamento.adapters.outbound.notifications;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResendNotificationAdapter { // ✨ Não implementa mais a interface global

    private final Resend resend;
    private final String fromEmail;

    public ResendNotificationAdapter(
            @Value("${resend.api.key}") String apiKey,
            @Value("${resend.from.email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    // Método Genérico Público
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(to)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            resend.emails().send(options);
            log.info("📧 E-mail enviado para: {}", to);
        } catch (ResendException e) {
            log.error("❌ Falha no Resend para {}: {}", to, e.getMessage());
        }
    }

    // Helper: E-mail de Boas-vindas
    public void sendWelcomeEmail(String email, String name) {
        String subject = "Bem-vindo ao Stylo! 🚀";
        String body = """
                <div style="font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 8px;">
                    <h2 style="color: #333;">Olá %s! 👋</h2>
                    <p>Estamos muito felizes em ter você conosco! O Stylo vai transformar a gestão do seu negócio.</p>
                    <p>Acesse seu painel para começar a configurar sua agenda.</p>
                    <div style="margin-top: 20px;">
                        <a href="https://stylo.app.br/dashboard" 
                           style="background-color: #000; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                           Ir para o Painel
                        </a>
                    </div>
                </div>
                """.formatted(name);
        sendEmail(email, subject, body);
    }
}