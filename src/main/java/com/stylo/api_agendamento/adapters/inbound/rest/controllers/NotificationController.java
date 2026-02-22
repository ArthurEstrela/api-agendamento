package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.context.SpringUserContext;
import com.stylo.api_agendamento.adapters.outbound.persistence.notification.JpaNotificationRepository;
import com.stylo.api_agendamento.adapters.outbound.persistence.notification.NotificationEntity;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final JpaNotificationRepository notificationRepository;
    private final SpringUserContext userContext;

    @GetMapping
    public ResponseEntity<List<NotificationEntity>> getMyNotifications() {
        UUID userId = userContext.getCurrentUserId();
        List<NotificationEntity> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        UUID userId = userContext.getCurrentUserId();
        NotificationEntity notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notificação não encontrada"));

        // Proteção: o utilizador só pode ler as suas próprias notificações
        if (notification.getUserId().equals(userId)) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        UUID userId = userContext.getCurrentUserId();
        List<NotificationEntity> unread = notificationRepository.findByUserIdAndIsReadFalse(userId);
        
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        
        return ResponseEntity.ok().build();
    }
}