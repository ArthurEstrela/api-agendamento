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
    private final IServiceProviderRepository serviceProviderRepository;
    private final IProductRepository productRepository;
    private final IFinancialRepository financialRepository;
    private final INotificationProvider notificationProvider;
    private final IClientRepository clientRepository; // ✨ Para resetar No-Show

    @Transactional
    public Appointment execute(CompleteAppointmentInput input) {
        // 1. Buscas e Validações Iniciais
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // Idempotência: Se já completou, retorna o objeto sem erro
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) return appointment;
        
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é possível finalizar um agendamento cancelado.");
        }

        Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        ServiceProvider provider = serviceProviderRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        // 2. Processamento de Produtos (Baixa de Estoque)
        if (input.soldProducts() != null && !input.soldProducts().isEmpty()) {
            processSoldProducts(appointment, input.soldProducts());
        }

        // 3. Cálculos Financeiros (Preço Final e Descontos)
        BigDecimal finalServicePrice = input.serviceFinalPrice() != null 
                ? input.serviceFinalPrice() 
                : appointment.calculateOriginalServiceTotal();

        // Calcula o desconto aplicado (Valor Original - Valor Cobrado)
        BigDecimal originalTotal = appointment.calculateOriginalServiceTotal();
        BigDecimal discount = originalTotal.subtract(finalServicePrice).max(BigDecimal.ZERO);

        // 4. Cálculo de Comissão
        BigDecimal commissionValue = BigDecimal.ZERO;
        if (provider.areCommissionsEnabled()) {
            // O domínio Professional deve saber calcular sua própria comissão
            commissionValue = professional.calculateCommissionFor(finalServicePrice);
        }

        // 5. Finaliza o Agendamento (Mudança de Estado no Domínio)
        // Isso atualiza status, define preço final, registra o snapshot da comissão
        appointment.complete(professional, discount, commissionValue); 
        appointment.setPaymentMethod(input.paymentMethod());

        // 6. ✨ ESTRATÉGIA DE RETENÇÃO: Resetar Contador de No-Show
        // Se o cliente compareceu e pagou, limpamos o histórico de faltas dele neste estabelecimento.
        clientRepository.findByUserAndProvider(appointment.getClientId(), appointment.getServiceProviderId())
                .ifPresent(client -> {
                    if (client.getNoShowCount() > 0) {
                        client.resetNoShow();
                        clientRepository.save(client);
                        log.info("Histórico de No-Show limpo para o cliente {}", client.getId());
                    }
                });

        // 7. Registro Financeiro (Revenue)
        financialRepository.registerRevenue(
            appointment.getServiceProviderId(),
            appointment.getFinalPrice(), // Valor real cobrado (Serviços + Produtos - Descontos)
            "Serviço #" + appointment.getId(),
            input.paymentMethod()
        );

        // 8. Persistência Final
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Agendamento finalizado. Comissão: R$ {}, Total: R$ {}", commissionValue, saved.getFinalPrice());

        // 9. Pós-Processamento (Review)
        requestClientReview(saved);

        return saved;
    }

    private void processSoldProducts(Appointment appointment, List<ProductSaleItem> items) {
        List<Product> productsToAdd = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (ProductSaleItem item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Produto " + item.productId() + " não encontrado."));
            
            // Regra de Domínio: Produto deduz seu próprio estoque (e valida quantidade <= 0)
            product.deductStock(item.quantity());
            productRepository.save(product); // Salva o novo estoque
            
            productsToAdd.add(product);
            quantities.add(item.quantity());
        }
        // Adiciona ao agendamento para recalcular o total
        appointment.addProducts(productsToAdd, quantities);
    }

    private void requestClientReview(Appointment appt) {
        try {
            String title = "Avalie seu atendimento ⭐";
            String link = "/dashboard?action=review&appointmentId=" + appt.getId();
            notificationProvider.sendNotification(appt.getClientId(), title, "Como foi sua experiência? Toque para avaliar.", link);
        } catch (Exception e) {
            // Falha na notificação não deve falhar a transação financeira
            log.error("Erro não-bloqueante ao pedir review: {}", e.getMessage());
        }
    }

    public record CompleteAppointmentInput(
            String appointmentId,
            PaymentMethod paymentMethod,
            BigDecimal serviceFinalPrice, // Preço editado manualmente pelo profissional (se permitido)
            List<ProductSaleItem> soldProducts
    ) {
        // Construtor canônico para garantir lista não nula
        public CompleteAppointmentInput {
            if (soldProducts == null) soldProducts = Collections.emptyList();
        }
    }

    public record ProductSaleItem(String productId, Integer quantity) {}
}