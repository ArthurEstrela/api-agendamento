package com.stylo.api_agendamento.core.domain.stock;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StockMovement {

    private UUID id;
    
    private UUID productId;
    private UUID providerId;
    
    private StockMovementType type;
    
    // Armazenamos sempre o valor ABSOLUTO (positivo). 
    // O sinal (+/-) é determinado pelo StockMovementType em tempo de leitura/cálculo.
    private Integer quantity; 
    
    private String reason;    // Ex: "Usado no cabelo da Dona Maria"
    
    private UUID performedByUserId; // Quem fez a movimentação (Auditoria)
    
    private LocalDateTime createdAt;

    // --- FACTORY ---

    public static StockMovement create(UUID productId, UUID providerId, StockMovementType type, 
                                       Integer quantity, String reason, UUID performedByUserId) {
        
        if (productId == null) throw new BusinessException("Produto é obrigatório.");
        if (providerId == null) throw new BusinessException("Prestador é obrigatório.");
        if (type == null) throw new BusinessException("Tipo de movimentação é obrigatório.");
        
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("A quantidade da movimentação deve ser positiva.");
        }

        if (performedByUserId == null) {
            throw new BusinessException("O usuário responsável pela movimentação é obrigatório.");
        }

        return StockMovement.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .providerId(providerId)
                .type(type)
                .quantity(quantity) // Armazena sempre positivo
                .reason(reason)
                .performedByUserId(performedByUserId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    /**
     * Retorna a quantidade com sinal (Positivo para entrada, Negativo para saída).
     * Útil para somatórios e relatórios.
     */
    public int getSignedQuantity() {
        return this.quantity * this.type.getMultiplier();
    }

    public boolean isEntry() {
        return this.type.isInput();
    }

    public boolean isExit() {
        return this.type.isOutput();
    }

    // --- IDENTIDADE ---
    // Movimentações de estoque geralmente são Value Objects imutáveis ou Entidades "append-only".
    // Uma vez criada, não se edita uma movimentação, se cria uma nova de compensação.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockMovement that = (StockMovement) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}