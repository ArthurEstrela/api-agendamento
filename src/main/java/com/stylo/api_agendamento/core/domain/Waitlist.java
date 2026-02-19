package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Waitlist {

    private UUID id;
    private UUID professionalId;
    private UUID serviceProviderId; // Importante para filtrar por estabelecimento
    
    private UUID clientId; // Opcional (pode ser um cliente não cadastrado ainda)
    private String clientName;
    private ClientPhone clientPhone;
    private String clientEmail;

    private LocalDate desiredDate; // O dia que ele quer a vaga
    private LocalDateTime requestTime; // Quando ele entrou na fila

    private boolean notified;
    private LocalDateTime notifiedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Waitlist create(UUID professionalId, UUID serviceProviderId, 
                                  String clientName, ClientPhone phone, String email, 
                                  LocalDate desiredDate) {
        
        if (professionalId == null) throw new BusinessException("Profissional é obrigatório.");
        if (clientName == null || clientName.isBlank()) throw new BusinessException("Nome do cliente é obrigatório.");
        if (desiredDate == null) throw new BusinessException("Data desejada é obrigatória.");
        
        if (desiredDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível entrar na lista de espera para uma data passada.");
        }

        return Waitlist.builder()
                .id(UUID.randomUUID())
                .professionalId(professionalId)
                .serviceProviderId(serviceProviderId)
                .clientName(clientName)
                .clientPhone(phone)
                .clientEmail(email)
                .desiredDate(desiredDate)
                .requestTime(LocalDateTime.now())
                .notified(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void markAsNotified() {
        if (this.notified) return;
        
        this.notified = true;
        this.notifiedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        // Se a data desejada já passou (ontem), a solicitação expirou
        return LocalDate.now().isAfter(this.desiredDate);
    }

    public void linkClient(UUID clientId) {
        this.clientId = clientId;
        this.updatedAt = LocalDateTime.now();
    }

    // --- IDENTIDADE ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Waitlist waitlist = (Waitlist) o;
        return Objects.equals(id, waitlist.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}