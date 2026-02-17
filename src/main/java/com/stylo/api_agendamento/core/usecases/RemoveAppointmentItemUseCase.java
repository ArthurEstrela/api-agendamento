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

@UseCase
@RequiredArgsConstructor
public class RemoveAppointmentItemUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProductRepository productRepository;

    @Transactional
    public Appointment execute(String appointmentId, String productId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Agendamento já finalizado.");
        }

        // 1. Encontra o item para saber a quantidade a devolver
        var itemOpt = appointment.getProducts().stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst();

        if (itemOpt.isPresent()) {
            Integer qtyToReturn = itemOpt.get().getQuantity();

            // 2. Devolve ao Estoque
            productRepository.findById(productId).ifPresent(product -> {
                product.restoreStock(qtyToReturn);
                productRepository.save(product);
            });

            // 3. Remove do Agendamento
            appointment.removeProduct(productId);
            return appointmentRepository.save(appointment);
        }

        return appointment; // Nada a fazer se não tinha o item
    }
}