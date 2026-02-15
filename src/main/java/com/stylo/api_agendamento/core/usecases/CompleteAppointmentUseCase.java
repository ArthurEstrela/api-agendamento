package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
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
@UseCase
@RequiredArgsConstructor
public class CompleteAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceProviderRepository serviceProviderRepository; // ✨ Novo Repo necessário
    private final IProductRepository productRepository;
    private final IFinancialRepository financialRepository;
    private final INotificationProvider notificationProvider;

    @Transactional
    public Appointment execute(CompleteAppointmentInput input) {
        // 1. Buscas Iniciais
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) return appointment;
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) 
            throw new BusinessException("Não é possível finalizar agendamento cancelado.");

        Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        // ✨ Busca o Estabelecimento para ver se comissão está ativa
        ServiceProvider provider = serviceProviderRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 2. Lógica de Produtos (Estoque) - Mantida da versão anterior
        if (input.soldProducts() != null && !input.soldProducts().isEmpty()) {
            List<Product> productsToAdd = new ArrayList<>();
            List<Integer> quantities = new ArrayList<>();
            for (ProductSaleItem item : input.soldProducts()) {
                Product product = productRepository.findById(item.productId())
                        .orElseThrow(() -> new EntityNotFoundException("Produto " + item.productId()));
                product.deductStock(item.quantity());
                productRepository.save(product);
                productsToAdd.add(product);
                quantities.add(item.quantity());
            }
            appointment.addProducts(productsToAdd, quantities);
        }

        // 3. Cálculos Financeiros (Preço Final)
        BigDecimal finalServicePrice = input.serviceFinalPrice() != null 
                ? input.serviceFinalPrice() 
                : appointment.calculateOriginalServiceTotal(); // Assume método auxiliar ou lógica de soma

        // Aplica desconto se houver
        BigDecimal originalTotal = appointment.calculateOriginalServiceTotal();
        BigDecimal discount = originalTotal.subtract(finalServicePrice).max(BigDecimal.ZERO);

        // 4. ✨ CÁLCULO DA COMISSÃO (O Pulo do Gato)
        BigDecimal commissionValue = BigDecimal.ZERO;

        if (provider.areCommissionsEnabled()) {
            // O profissional calcula baseado na estratégia (Porcentagem ou Fixo)
            commissionValue = professional.calculateCommissionFor(finalServicePrice);
        }

        // 5. Finaliza o Agendamento (Domínio)
        // O método complete() do Appointment deve receber a comissão para salvar o snapshot
        appointment.complete(professional, discount, commissionValue); 
        appointment.setPaymentMethod(input.paymentMethod());

        // 6. Persistência e Financeiro
        financialRepository.registerRevenue(
            appointment.getServiceProviderId(),
            appointment.getFinalPrice(),
            "Serviço #" + appointment.getId(),
            input.paymentMethod()
        );
        
        // Opcional: Registrar a saída (Despesa) da comissão imediatamente ou deixar para o fechamento
        // financialRepository.registerExpense(... commissionValue ...); 

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Agendamento finalizado. Comissão gerada: R$ {}", commissionValue);

        // 7. Notificação
        requestClientReview(saved);

        return saved;
    }

    private void requestClientReview(Appointment appt) {
        try {
            String title = "Avalie seu atendimento ⭐";
            String link = "/dashboard?action=review&appointmentId=" + appt.getId();
            notificationProvider.sendNotification(appt.getClientId(), title, "Como foi sua experiência?", link);
        } catch (Exception e) {
            log.error("Erro ao pedir review: {}", e.getMessage());
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