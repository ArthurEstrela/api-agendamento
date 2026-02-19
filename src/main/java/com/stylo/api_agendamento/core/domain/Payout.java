package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Payout {

    private UUID id;
    private UUID professionalId;
    private UUID serviceProviderId;

    private BigDecimal totalAmount;

    @Builder.Default
    private List<UUID> appointmentIds = new ArrayList<>(); // IDs consolidados neste pagamento

    private LocalDateTime processedAt;

    private PayoutStatus status; // PENDING, PROCESSING, PAID, FAILED

    private String externalTransferId; // ID da transação no banco/Stripe
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static Payout create(UUID professionalId, UUID serviceProviderId, 
                                BigDecimal totalAmount, List<UUID> appointmentIds) {
        
        if (professionalId == null) throw new BusinessException("O pagamento deve ser destinado a um profissional.");
        if (serviceProviderId == null) throw new BusinessException("O pagamento deve ter um prestador de origem.");
        
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor do pagamento deve ser maior que zero.");
        }

        if (appointmentIds == null || appointmentIds.isEmpty()) {
            throw new BusinessException("O pagamento deve estar vinculado a pelo menos um agendamento.");
        }

        return Payout.builder()
                .id(UUID.randomUUID()) // Identidade gerada
                .professionalId(professionalId)
                .serviceProviderId(serviceProviderId)
                .totalAmount(totalAmount)
                .appointmentIds(new ArrayList<>(appointmentIds)) // Cópia defensiva
                .status(PayoutStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void markAsProcessing() {
        if (this.status != PayoutStatus.PENDING) {
            throw new BusinessException("Apenas pagamentos pendentes podem ser processados.");
        }
        this.status = PayoutStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsPaid(String externalTransferId) {
        if (this.status == PayoutStatus.PAID) return; // Idempotente

        this.status = PayoutStatus.PAID;
        this.externalTransferId = externalTransferId;
        this.processedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.failureReason = null; // Limpa erro anterior se houver
    }

    public void markAsFailed(String reason) {
        if (this.status == PayoutStatus.PAID) {
            throw new BusinessException("Não é possível falhar um pagamento já realizado.");
        }
        this.status = PayoutStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payout payout = (Payout) o;
        return Objects.equals(id, payout.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- ENUMS ---

    public enum PayoutStatus {
        PENDING,    // Criado, aguardando processamento
        PROCESSING, // Enviado para o gateway (Stripe/Banco)
        PAID,       // Confirmado com sucesso
        FAILED      // Erro na transferência
    }
}