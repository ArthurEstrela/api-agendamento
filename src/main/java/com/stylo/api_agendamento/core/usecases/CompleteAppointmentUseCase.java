package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.stock.StockMovement;
import com.stylo.api_agendamento.core.domain.stock.StockMovementType;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CompleteAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final IServiceProviderRepository serviceProviderRepository;
    private final IProductRepository productRepository;
    private final IStockMovementRepository stockMovementRepository;
    private final IFinancialRepository financialRepository;
    private final INotificationProvider notificationProvider;
    private final IClientRepository clientRepository;
    private final IUserContext userContext;

    @Transactional
    public Appointment execute(Input input) {
        // 1. Buscas e Validações Iniciais
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) return appointment;
        
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é possível finalizar um agendamento cancelado.");
        }

        Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado."));

        ServiceProvider provider = serviceProviderRepository.findById(appointment.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Estabelecimento não encontrado."));

        UUID operatorId = userContext.getCurrentUserId();

        // 2. Processamento de Produtos Adicionais no Checkout (Baixa de Estoque e Kardex)
        if (!input.soldProducts().isEmpty()) {
            processSoldProducts(appointment, input.soldProducts(), operatorId);
        }

        // 3. Cálculos Financeiros
        BigDecimal finalServicePrice = input.serviceFinalPrice() != null 
                ? input.serviceFinalPrice() 
                : appointment.calculateOriginalServiceTotal();

        BigDecimal originalTotal = appointment.calculateOriginalServiceTotal();
        BigDecimal discount = originalTotal.subtract(finalServicePrice).max(BigDecimal.ZERO);

        // 4. Cálculo de Comissão
        BigDecimal commissionValue = provider.isCommissionsEnabled() 
                ? professional.calculateCommissionFor(finalServicePrice) 
                : BigDecimal.ZERO;

        // 5. Finaliza no Domínio
        appointment.complete(professional, discount, commissionValue); 
        appointment.setPaymentMethod(input.paymentMethod());

        // 6. Estratégia de Fidelização: Reset de No-Show
        clientRepository.findByUserAndProvider(appointment.getClientId(), appointment.getServiceProviderId())
                .ifPresent(client -> {
                    if (client.getNoShowCount() > 0) {
                        client.resetNoShow();
                        clientRepository.save(client);
                        log.info("Histórico de No-Show limpo para o cliente {}", client.getName());
                    }
                });

        // 7. Registro de Receita Financeira
        financialRepository.registerRevenue(
            appointment.getServiceProviderId(),
            appointment.getFinalPrice(),
            "Atendimento #" + appointment.getId().toString().substring(0, 8),
            input.paymentMethod()
        );

        // 8. Persistência
        Appointment saved = appointmentRepository.save(appointment);
        
        // 9. Pós-Processamento: Solicitar Avaliação (Push)
        requestClientReview(saved);

        log.info("Agendamento {} finalizado com sucesso. Total: R$ {}", saved.getId(), saved.getFinalPrice());
        return saved;
    }

    private void processSoldProducts(Appointment appointment, List<ProductSaleItem> items, UUID operatorId) {
        List<Product> productsToAdd = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        for (ProductSaleItem item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Produto " + item.productId() + " não encontrado."));
            
            // Regra: Produto deve ser do mesmo estabelecimento
            if (!product.getServiceProviderId().equals(appointment.getServiceProviderId())) {
                throw new BusinessException("O produto " + product.getName() + " não pertence a este estabelecimento.");
            }

            product.deductStock(item.quantity());
            productRepository.save(product);

            // Registro no Kardex
            StockMovement movement = StockMovement.create(
                    product.getId(),
                    product.getServiceProviderId(),
                    StockMovementType.SALE,
                    item.quantity(),
                    "Venda vinculada ao Agendamento #" + appointment.getId().toString().substring(0, 8),
                    operatorId
            );
            stockMovementRepository.save(movement);
            
            productsToAdd.add(product);
            quantities.add(item.quantity());
        }
        appointment.addProducts(productsToAdd, quantities);
    }

    private void requestClientReview(Appointment appt) {
        try {
            notificationProvider.sendPushNotification(
                appt.getClientId(), 
                "Avalie seu atendimento ⭐", 
                "Como foi sua experiência no " + appt.getBusinessName() + "? Toque para avaliar.", 
                "/reviews/new?appointmentId=" + appt.getId()
            );
        } catch (Exception e) {
            log.error("Erro não-bloqueante ao solicitar avaliação: {}", e.getMessage());
        }
    }

    public record Input(
            UUID appointmentId,
            PaymentMethod paymentMethod,
            BigDecimal serviceFinalPrice,
            List<ProductSaleItem> soldProducts
    ) {
        public Input {
            if (soldProducts == null) soldProducts = Collections.emptyList();
        }
    }

    public record ProductSaleItem(UUID productId, Integer quantity) {}
}