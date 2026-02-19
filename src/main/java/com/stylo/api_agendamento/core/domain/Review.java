package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Review {

    private UUID id;
    
    // Vínculos imutáveis
    private UUID appointmentId;
    private UUID clientId;
    private UUID serviceProviderId;
    private UUID professionalId;

    // Snapshots (Nomes gravados no momento da avaliação para histórico)
    private String clientName;
    private String professionalName; 

    private Integer rating; 
    private String comment;

    // ✨ NOVO: Funcionalidade de Réplica (O dono do salão responde)
    private String reply;
    private LocalDateTime repliedAt;

    // ✨ NOVO: Moderação (Ocultar comentários ofensivos)
    private boolean isHidden;
    private String moderationReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Review create(Appointment appointment, int rating, String comment) {
        // REGRA DE NEGÓCIO: Só avalia o que foi concluído
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new BusinessException("Apenas agendamentos finalizados podem ser avaliados.");
        }

        // Validação de nota
        if (rating < 1 || rating > 5) {
            throw new BusinessException("A avaliação deve ser entre 1 e 5 estrelas.");
        }

        // Validação de comentário (opcional, mas não pode ser gigante)
        if (comment != null && comment.length() > 500) {
            throw new BusinessException("O comentário não pode exceder 500 caracteres.");
        }

        return Review.builder()
                .id(UUID.randomUUID())
                .appointmentId(appointment.getId())
                .clientId(appointment.getClientId())
                .clientName(appointment.getClientName()) 
                .serviceProviderId(appointment.getServiceProviderId()) // Ajustado para o nome correto do campo no Appointment
                .professionalId(appointment.getProfessionalId())
                .professionalName(appointment.getProfessionalName()) 
                .rating(rating)
                .comment(comment)
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void respond(String replyText) {
        if (replyText == null || replyText.isBlank()) {
            throw new BusinessException("A resposta não pode ser vazia.");
        }
        if (this.isHidden) {
            throw new BusinessException("Não é possível responder a uma avaliação oculta.");
        }
        
        this.reply = replyText;
        this.repliedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void hide(String reason) {
        this.isHidden = true;
        this.moderationReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void unhide() {
        this.isHidden = false;
        this.moderationReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasComment() {
        return this.comment != null && !this.comment.isBlank();
    }

    public boolean hasReply() {
        return this.reply != null && !this.reply.isBlank();
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return Objects.equals(id, review.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}