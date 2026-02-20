package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import com.stylo.api_agendamento.core.domain.stock.StockMovementType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import com.stylo.api_agendamento.core.ports.IStockMovementRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class RemoveAppointmentItemUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProductRepository productRepository;
    private final IStockMovementRepository stockMovementRepository;
    private final IUserContext userContext;

    @Transactional
    public Appointment execute(UUID appointmentId, UUID productId) {
        // 1. Busca Agendamento
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus().isTerminalState()) {
            throw new BusinessException("Não é possível remover itens de um agendamento finalizado ou cancelado.");
        }

        // 2. Localiza o item para processar a devolução
        var itemOpt = appointment.getProducts().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (itemOpt.isPresent()) {
            Integer qtyToReturn = itemOpt.get().getQuantity();
            UUID operatorId = userContext.getCurrentUserId();

            // 3. Devolve ao Estoque e Registra Auditoria (Kardex)
            productRepository.findById(productId).ifPresent(product -> {
                product.addStock(qtyToReturn);
                productRepository.save(product);

                StockMovement movement = StockMovement.create(
                        product.getId(),
                        product.getServiceProviderId(),
                        StockMovementType.RETURN_FROM_CUSTOMER,
                        qtyToReturn,
                        "Remoção de item da Comanda #" + appointment.getId().toString().substring(0, 8),
                        operatorId
                );
                stockMovementRepository.save(movement);
            });

            // 4. Remove do Agendamento (Domínio recalcula totais automaticamente)
            appointment.removeProduct(productId);
            return appointmentRepository.save(appointment);
        }

        return appointment; 
    }
}