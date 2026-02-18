package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.domain.events.ProductLowStockEvent;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IEventPublisher;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class AddAppointmentItemUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProductRepository productRepository;
    private final IEventPublisher eventPublisher; // ✨ Injeção do Publicador de Eventos

    @Transactional
    public Appointment execute(String appointmentId, String productId, Integer quantity) {
        // 1. Validações Iniciais
        if (quantity == null || quantity <= 0) {
            throw new BusinessException("A quantidade deve ser positiva.");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        // Regra de Negócio: Não mexe em agendamento fechado
        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é possível alterar itens de um agendamento finalizado ou cancelado.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Produto não encontrado."));

        // 2. Regra de Estoque: Deduz imediatamente para reservar o item físico
        product.deductStock(quantity);
        productRepository.save(product);

        // ✨ 3. Verificação de Alerta: Dispara evento se atingiu o mínimo
        if (product.isBelowMinStock()) {
            eventPublisher.publish(new ProductLowStockEvent(
                    product.getId(),
                    product.getServiceProviderId(),
                    product.getName(),
                    product.getStockQuantity(),
                    product.getMinStockAlert()
            ));
        }

        // 4. Adiciona ao Carrinho do Agendamento (Atualiza preço total)
        appointment.addProducts(List.of(product), List.of(quantity));
        
        return appointmentRepository.save(appointment);
    }
}