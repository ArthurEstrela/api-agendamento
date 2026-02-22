package com.stylo.api_agendamento.adapters.outbound.persistence.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<NotificationEntity> findByUserIdAndIsReadFalse(UUID userId);
}