package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.domain.events.ProductLowStockEvent;
import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import com.stylo.api_agendamento.core.domain.stock.StockMovementType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class RegisterInternalConsumptionUseCase {

    private final IProductRepository productRepository;
    private final IStockMovementRepository stockMovementRepository; // Novo Port
    private final IUserContext userContext;
    private final IEventPublisher eventPublisher;

    @Transactional
    public void execute(String productId, Integer quantity, String reason) {
        if (quantity <= 0) throw new BusinessException("Quantidade deve ser positiva.");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Produto não encontrado."));

        // 1. Baixa no Estoque (Core Domain)
        product.deductStock(quantity);
        productRepository.save(product);

        // 2. Registro de Auditoria (Movement)
        StockMovement movement = StockMovement.builder()
                .id(UUID.randomUUID().toString())
                .productId(product.getId())
                .providerId(product.getServiceProviderId())
                .type(StockMovementType.INTERNAL_USE)
                .quantity(-quantity) // Negativo pois é saída
                .reason(reason)
                .performedByUserId(userContext.getCurrentUserId())
                .createdAt(LocalDateTime.now())
                .build();
        
        stockMovementRepository.save(movement);

        // 3. Verificação de Alerta
        if (product.isBelowMinStock()) {
            eventPublisher.publish(new ProductLowStockEvent(
                product.getId(),
                product.getServiceProviderId(),
                product.getName(),
                product.getStockQuantity(),
                product.getMinStockAlert()
            ));
        }
    }
}