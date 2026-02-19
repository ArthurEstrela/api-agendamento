package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.*;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.domain.financial.CashTransactionType;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ProcessPosCheckoutUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final ManageCashRegisterUseCase manageCashRegisterUseCase;
    private final ApplyCouponUseCase applyCouponUseCase;
    private final ICouponRepository couponRepository;
    private final IUserContext userContext;

    @Transactional
    public Response execute(Input input) {
        UUID operatorId = userContext.getCurrentUserId();

        // 1. Busca Agendamento e Profissional
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        if (appointment.getStatus().isTerminalState()) {
            throw new BusinessException("Esta comanda já foi finalizada ou cancelada.");
        }

        Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional vinculado não encontrado."));

        // 2. Consolidação de Totais (Serviços + Produtos)
        BigDecimal baseTotal = appointment.calculateOriginalServiceTotal();
        if (appointment.hasProducts()) {
             baseTotal = baseTotal.add(appointment.calculateProductsTotal());
        }

        // 3. Aplicação de Cupom de Checkout
        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = null;

        if (input.couponCode() != null && !input.couponCode().isBlank()) {
            var result = applyCouponUseCase.validateAndCalculate(
                    input.couponCode(), 
                    appointment.getServiceProviderId(), 
                    baseTotal
            );
            coupon = result.coupon();
            discount = result.discountAmount();
        } else {
            discount = appointment.getDiscountAmount() != null ? appointment.getDiscountAmount() : BigDecimal.ZERO;
        }

        BigDecimal finalPrice = baseTotal.subtract(discount).max(BigDecimal.ZERO);
        
        // 4. Cálculo de Troco (Se pago em dinheiro)
        BigDecimal change = BigDecimal.ZERO;
        if (input.amountGiven() != null && input.method() == PaymentMethod.CASH) {
            if (input.amountGiven().compareTo(finalPrice) < 0) {
                throw new BusinessException("Valor entregue é insuficiente para cobrir o total de R$ " + finalPrice);
            }
            change = input.amountGiven().subtract(finalPrice);
        }

        // 5. Registro no Caixa Físico (Gaveta)
        if (input.method() == PaymentMethod.CASH) {
            manageCashRegisterUseCase.addOperation(
                    CashTransactionType.SALE,
                    finalPrice,
                    "Checkout Comanda #" + appointment.getId().toString().substring(0, 8)
            );
        }

        // 6. Finalização do Agendamento no Domínio
        BigDecimal commission = professional.calculateCommissionFor(finalPrice);
        
        appointment.setPaymentMethod(input.method());
        appointment.setFinalPrice(finalPrice);
        appointment.setDiscountAmount(discount);
        if (coupon != null) appointment.setCouponId(coupon.getId());
        
        // Registra pagamento manual/local
        appointment.confirmPayment("POS-OFFLINE-" + UUID.randomUUID().toString().substring(0, 8));
        appointment.complete(professional, discount, commission);

        // Atualiza uso do cupom se aplicado
        if (coupon != null) {
            coupon.incrementUsage();
            couponRepository.save(coupon);
        }

        appointmentRepository.save(appointment);
        log.info("Checkout PDV concluído para agendamento {}. Total: R$ {}", appointment.getId(), finalPrice);

        return new Response(appointment, change);
    }

    public record Input(
            UUID appointmentId,
            PaymentMethod method,
            BigDecimal amountGiven,
            String couponCode
    ) {}

    public record Response(
            Appointment appointment,
            BigDecimal change
    ) {}
}