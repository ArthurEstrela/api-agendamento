package com.stylo.api_agendamento.adapters.outbound.notifications;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Slf4j
@Component
public class FcmNotificationAdapter {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(
                                new ClassPathResource("serviceAccountKey.json").getInputStream()))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK inicializado.");
            }
        } catch (IOException e) {
            log.error("Erro ao inicializar Firebase: {}", e.getMessage());
        }
    }

    public void sendPush(String token, String title, String body, String actionUrl) {
        if (token == null || token.isBlank()) return;

        var notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        var messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification);

        // ✨ Adiciona o link no payload para o App navegar
        if (actionUrl != null && !actionUrl.isBlank()) {
            messageBuilder.putData("link", actionUrl);
            messageBuilder.putData("click_action", "FLUTTER_NOTIFICATION_CLICK");
        }

        try {
            FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("Push enviado para token: {}...", token.substring(0, 10));
        } catch (Exception e) {
            log.error("Erro ao enviar Push: {}", e.getMessage());
        }
    }
}