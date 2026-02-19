package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class AddAppointmentItemUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProductRepository productRepository;
    private final IStockMovementRepository stockMovementRepository; // ✨ Novo: Histórico de Estoque
    private final IEventPublisher eventPublisher;
    private final IUserContext userContext; // ✨ Novo: Auditoria

    @Transactional
    public Appointment execute(Input input) {
        // 1. Validação de Entrada
        if (input.quantity() == null || input.quantity() <= 0) {
            throw new BusinessException("A quantidade deve ser positiva.");
        }

        UUID userId = userContext.getCurrentUserId();

        // 2. Busca e Validação do Agendamento
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // Regra: Não pode alterar histórico
        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é possível adicionar itens a um agendamento finalizado ou cancelado.");
        }

        // 3. Busca e Validação do Produto
        Product product = productRepository.findById(input.productId())
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado."));

        // ✨ Segurança: Tenant Isolation (Produto deve pertencer ao mesmo salão do agendamento)
        if (!product.getServiceProviderId().equals(appointment.getServiceProviderId())) {
            log.warn("Tentativa de Cross-Tenant: User {} tentou adicionar Produto {} ao Agendamento {}", 
                    userId, product.getId(), appointment.getId());
            throw new BusinessException("Este produto não pertence ao estabelecimento do agendamento.");
        }

        if (!product.isActive()) {
            throw new BusinessException("Este produto está inativo e não pode ser vendido.");
        }

        // 4. Lógica de Estoque (Domínio)
        // O método deductStock já valida se tem quantidade suficiente
        product.deductStock(input.quantity());
        productRepository.save(product);

        // 5. Auditoria de Estoque (Criação do Movimento)
        // Isso gera o histórico: "Saiu 2 shampoos por causa do Agendamento X"
        StockMovement movement = StockMovement.create(
                product.getId(),
                product.getServiceProviderId(),
                StockMovementType.SALE, // Tipo: Venda
                input.quantity(),
                "Venda via Agendamento #" + appointment.getId().toString().substring(0, 8),
                userId
        );
        stockMovementRepository.save(movement);

        // 6. Verifica Alerta de Estoque Baixo
        if (product.isBelowMinStock()) {
            eventPublisher.publish(new ProductLowStockEvent(
                    product.getId(),
                    product.getServiceProviderId(),
                    product.getName(),
                    product.getStockQuantity(),
                    product.getMinStockAlert()
            ));
            log.info("Alerta de estoque baixo disparado para o produto: {}", product.getName());
        }

        // 7. Adiciona ao Agendamento (Carrinho)
        appointment.addProducts(
                Collections.singletonList(product), 
                Collections.singletonList(input.quantity())
        );
        
        // Salva e retorna o agendamento atualizado (com novos totais calculados)
        Appointment updatedAppointment = appointmentRepository.save(appointment);
        
        log.info("Item adicionado ao agendamento {}: Produto={}, Qtd={}", 
                appointment.getId(), product.getName(), input.quantity());

        return updatedAppointment;
    }

    public record Input(
            UUID appointmentId,
            UUID productId,
            Integer quantity
    ) {}
}