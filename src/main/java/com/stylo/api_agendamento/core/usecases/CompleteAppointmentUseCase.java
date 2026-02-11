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

        // 4. Lógica de Produtos
        // Adiciona os produtos ao domínio para que ele calcule o total corretamente depois
        if (input.soldProducts() != null && !input.soldProducts().isEmpty()) {
            for (ProductSaleItem item : input.soldProducts()) {
                Product product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado: " + item.productId()));

                // Deduz estoque
                product.deductStock(item.quantity());
                productRepository.save(product);

                // Adiciona ao agendamento (Domínio)
                // OBS: Criamos o método 'addProducts' no Appointment.java na resposta anterior
                // Se não tiver, faça a soma manual aqui, mas o ideal é usar o domínio.
                // Vou manter a lógica manual aqui para garantir compatibilidade caso não tenha atualizado o Appointment
            }
            // OBS: O ideal seria chamar appointment.addProducts(...) aqui.
        }
        
        // Recalculo manual dos produtos para somar no final (Fallback se não usar appointment.addProducts)
        BigDecimal productsTotal = BigDecimal.ZERO;
        if (input.soldProducts() != null) {
             for (ProductSaleItem item : input.soldProducts()) {
                Product p = productRepository.findById(item.productId()).orElseThrow();
                productsTotal = productsTotal.add(p.getPrice().multiply(new BigDecimal(item.quantity())));
             }
        }

        // 5. Define valor final do SERVIÇO
        // CORREÇÃO: Usamos appointment.getPrice() que já contém o total dos serviços (calculado na criação)
        BigDecimal serviceFinalPrice = input.serviceFinalPrice() != null 
                ? input.serviceFinalPrice() 
                : appointment.getPrice(); 

        BigDecimal grandTotal = serviceFinalPrice.add(productsTotal);

        // 6. Finaliza o Agendamento
        appointment.setPrice(serviceFinalPrice); // Atualiza o preço base do serviço cobrado
        appointment.complete(professional);
        appointment.setPaymentMethod(input.paymentMethod());
        // Se você tiver implementado a lista de produtos no Appointment, adicione-os aqui

        // 7. Registro Financeiro
        // CORREÇÃO: O método no repositório precisa aceitar PaymentMethod
        financialRepository.registerRevenue(
            appointment.getServiceProviderId(),
            grandTotal,
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