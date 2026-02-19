package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.domain.events.ProductLowStockEvent;
import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import com.stylo.api_agendamento.core.domain.stock.StockMovementType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class RegisterInternalConsumptionUseCase {

    private final IProductRepository productRepository;
    private final IStockMovementRepository stockMovementRepository;
    private final IUserContext userContext;
    private final IEventPublisher eventPublisher;

    @Transactional
    public void execute(UUID productId, Integer quantity, String reason) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("A quantidade de consumo deve ser maior que zero.");
        }

        if (reason == null || reason.isBlank()) {
            throw new BusinessException("É obrigatório informar o motivo do consumo interno.");
        }

        // 1. Busca e Valida Produto
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produto não localizado para baixa de estoque."));

        UUID operatorId = userContext.getCurrentUserId();

        // 2. Lógica de Domínio (Deduct Stock)
        product.deductStock(quantity);
        productRepository.save(product);

        // 3. Registro de Auditoria (Kardex)
        // Usamos o factory que já define o StockMovementType.INTERNAL_USE
        StockMovement movement = StockMovement.create(
                product.getId(),
                product.getServiceProviderId(),
                StockMovementType.INTERNAL_USE,
                quantity,
                "Consumo Interno: " + reason,
                operatorId
        );
        stockMovementRepository.save(movement);

        log.info("Consumo interno registrado: {} unidades de '{}'. Motivo: {}", quantity, product.getName(), reason);

        // 4. Verificação de Alerta de Reposição
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