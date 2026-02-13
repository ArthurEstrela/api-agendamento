package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CompleteAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IProductRepository productRepository;
    private final IFinancialRepository financialRepository;
    private final INotificationProvider notificationProvider; // ✨ Injetado para pedir Review

    @Transactional
    public Appointment execute(CompleteAppointmentInput input) {
        // 1. Busca o agendamento
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Validação de Estado
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            return appointment;
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é possível finalizar um agendamento cancelado.");
        }

        // 3. Busca o profissional
        Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // 4. Lógica de Produtos (Estoque e Associação)
        if (input.soldProducts() != null && !input.soldProducts().isEmpty()) {
            List<Product> productsToAdd = new ArrayList<>();
            List<Integer> quantities = new ArrayList<>();

            for (ProductSaleItem item : input.soldProducts()) {
                Product product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado: " + item.productId()));

                // Deduz estoque
                product.deductStock(item.quantity());
                productRepository.save(product);

                productsToAdd.add(product);
                quantities.add(item.quantity());
            }
            appointment.addProducts(productsToAdd, quantities);
        }

        // 5. Cálculo Financeiro e Descontos
        BigDecimal finalServicePriceToCharge = input.serviceFinalPrice() != null 
                ? input.serviceFinalPrice() 
                : appointment.getServices().stream().map(s -> s.getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal originalServicePrice = appointment.getServices().stream()
                .map(s -> s.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal discount = originalServicePrice.subtract(finalServicePriceToCharge);
        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;

        // 6. Finaliza o Agendamento (Domínio)
        appointment.complete(professional, discount);
        appointment.setPaymentMethod(input.paymentMethod());

        // 7. Registro Financeiro
        financialRepository.registerRevenue(
            appointment.getServiceProviderId(),
            appointment.getFinalPrice(),
            "Receita de Agendamento #" + appointment.getId(),
            input.paymentMethod()
        );

        Appointment savedAppointment = appointmentRepository.save(appointment);
        log.info("Agendamento {} finalizado com sucesso.", savedAppointment.getId());

        // ✨ 8. Pós-Venda: Solicitar Avaliação (Notificação com Deep Link)
        requestClientReview(savedAppointment);

        return savedAppointment;
    }

    private void requestClientReview(Appointment appt) {
        try {
            String title = "Como foi seu atendimento? ⭐";
            String body = String.format("O serviço com %s foi concluído. Toque para avaliar!", 
                    appt.getProfessionalName());
            
            // Deep Link para abrir o modal de review no App
            String reviewLink = String.format("/dashboard?action=review&appointmentId=%s", appt.getId());

            notificationProvider.sendNotification(appt.getClientId(), title, body, reviewLink);
        } catch (Exception e) {
            log.error("Erro ao solicitar review: {}", e.getMessage());
        }
    }

    public record CompleteAppointmentInput(
            String appointmentId,
            PaymentMethod paymentMethod,
            BigDecimal serviceFinalPrice,
            List<ProductSaleItem> soldProducts
    ) {
        public CompleteAppointmentInput {
            if (soldProducts == null) soldProducts = Collections.emptyList();
        }
    }

    public record ProductSaleItem(String productId, Integer quantity) {}
}