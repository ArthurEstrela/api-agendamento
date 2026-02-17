package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class AddAppointmentItemUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProductRepository productRepository;

    @Transactional
    public Appointment execute(String appointmentId, String productId, Integer quantity) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED || appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é possível alterar itens de um agendamento finalizado ou cancelado.");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Produto não encontrado."));

        // 1. Regra de Estoque: Deduz imediatamente para reservar o item físico
        product.deductStock(quantity);
        productRepository.save(product);

        // 2. Adiciona ao Carrinho do Agendamento
        appointment.addProducts(List.of(product), List.of(quantity));
        
        return appointmentRepository.save(appointment);
    }
}