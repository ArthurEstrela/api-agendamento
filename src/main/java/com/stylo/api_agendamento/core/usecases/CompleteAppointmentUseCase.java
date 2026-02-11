package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.AppointmentStatus;
import com.stylo.api_agendamento.core.domain.Product;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import com.stylo.api_agendamento.core.ports.IProductRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class CompleteAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IProductRepository productRepository;
    private final IFinancialRepository financialRepository;

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

        // 4. Lógica de Produtos (Otimizada)
        if (input.soldProducts() != null && !input.soldProducts().isEmpty()) {
            List<Product> productsToAdd = new ArrayList<>();
            List<Integer> quantities = new ArrayList<>();

            for (ProductSaleItem item : input.soldProducts()) {
                Product product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado: " + item.productId()));

                // Deduz estoque e salva o produto atualizado
                product.deductStock(item.quantity());
                productRepository.save(product);

                // Prepara listas para o domínio
                productsToAdd.add(product);
                quantities.add(item.quantity());
            }
            
            // O PULO DO GATO: Passa para o domínio criar os AppointmentItems e calcular totais
            appointment.addProducts(productsToAdd, quantities);
        }

        // 5. Finaliza (O domínio já tem o preço base somado em 'price')
        // Se serviceFinalPrice vier nulo, usa o preço original dos serviços (calculado no create)
        // OBS: appointment.getServices() soma o total original.
        // Aqui assumimos que o input.serviceFinalPrice é o valor COBRADO pelo serviço (com desconto ou não).
        BigDecimal finalServicePriceToCharge = input.serviceFinalPrice() != null 
                ? input.serviceFinalPrice() 
                : appointment.getServices().stream().map(s -> s.getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Subtrai do total calculado pelo domínio a diferença (desconto) se houver, 
        // ou passa o desconto explicitamente para o método complete.
        // Simplificação: Passamos o desconto para o complete
        
        // Calculamos o desconto dado no serviço: (Preço Original Serviço - Preço Final Serviço)
        BigDecimal originalServicePrice = appointment.getServices().stream()
                .map(s -> s.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal discount = originalServicePrice.subtract(finalServicePriceToCharge);
        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO; // Evita desconto negativo (acréscimo)

        // 6. Finaliza o Agendamento no Domínio
        appointment.complete(professional, discount);
        appointment.setPaymentMethod(input.paymentMethod());

        // 7. Registro Financeiro (Usa o finalPrice calculado pelo domínio que já soma produtos + serviço com desconto)
        financialRepository.registerRevenue(
            appointment.getServiceProviderId(),
            appointment.getFinalPrice(), // Valor exato cobrado
            "Receita de Agendamento #" + appointment.getId(),
            input.paymentMethod()
        );

        return appointmentRepository.save(appointment);
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