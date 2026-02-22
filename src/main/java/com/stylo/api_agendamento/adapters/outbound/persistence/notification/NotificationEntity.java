package com.stylo.api_agendamento.adapters.outbound.persistence.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    private String title;
    private String message;
    private String link;
    
    @Column(name = "is_read")
    private boolean isRead;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}