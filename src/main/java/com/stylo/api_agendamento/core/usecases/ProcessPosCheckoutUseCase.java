package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.domain.financial.CashTransactionType;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class ProcessPosCheckoutUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IProfessionalRepository professionalRepository;
    private final ManageCashRegisterUseCase manageCashRegisterUseCase;
    private final IUserContext userContext;
    private final ApplyCouponUseCase applyCouponUseCase;
    private final ICouponRepository couponRepository;

    @Transactional
    public CheckoutResult execute(CheckoutInput input) {
        User receptionist = userContext.getCurrentUser();

        // 1. Recuperar Agendamento
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new BusinessException("Comanda não encontrada."));

        // ✨ CORREÇÃO 1: O método agora existe no Enum
        if (appointment.getStatus().isTerminalState()) {
            throw new BusinessException("Esta comanda já foi finalizada ou cancelada.");
        }

        Professional professional = professionalRepository.findById(appointment.getProfessionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 2. Calcular Totais e Aplicar Cupons
        BigDecimal totalAmount = appointment.calculateOriginalServiceTotal();
        if (appointment.hasProducts()) {
             totalAmount = totalAmount.add(
                 appointment.getProducts().stream()
                     .map(Appointment.AppointmentItem::getTotal)
                     .reduce(BigDecimal.ZERO, BigDecimal::add)
             );
        }

        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = null;

        String codeToApply = input.couponCode();
        if (codeToApply != null && !codeToApply.isBlank()) {
            var result = applyCouponUseCase.validateAndCalculate(codeToApply, appointment.getProviderId(), totalAmount);
            coupon = result.coupon();
            discount = result.discountAmount();
        } else if (appointment.getDiscountAmount() != null && appointment.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            discount = appointment.getDiscountAmount();
        }

        // 3. Validar Pagamento
        BigDecimal finalPrice = totalAmount.subtract(discount).max(BigDecimal.ZERO);
        
        BigDecimal change = BigDecimal.ZERO;
        if (input.amountGiven() != null) {
            if (input.amountGiven().compareTo(finalPrice) < 0) {
                throw new BusinessException("Valor pago é menor que o total da comanda.");
            }
            change = input.amountGiven().subtract(finalPrice);
        }

        // 4. Integração com CAIXA FÍSICO
        if (input.method() == PaymentMethod.CASH) {
            manageCashRegisterUseCase.addOperation(
                    receptionist,
                    CashTransactionType.SALE,
                    finalPrice,
                    "Venda Comanda #" + appointment.getId().substring(0, 8)
            );
        }

        // 5. Finalizar Agendamento (Persistência)
        
        // ✨ CORREÇÃO 2: Delega o cálculo para o Domínio (Professional)
        // Isso usa a lógica de RemunerationType (FIXED ou PERCENTAGE) que já configuramos
        BigDecimal commission = professional.calculateCommissionFor(finalPrice);
        
        appointment.setPaymentMethod(input.method());
        appointment.setFinalPrice(finalPrice);
        appointment.setDiscountAmount(discount);
        if (coupon != null) appointment.setCouponId(coupon.getId());
        
        appointment.confirmPayment("POS-" + System.currentTimeMillis());
        appointment.complete(professional, discount, commission);

        if (coupon != null) {
            coupon.incrementUsage();
            couponRepository.save(coupon);
        }

        appointmentRepository.save(appointment);

        return new CheckoutResult(appointment, change);
    }

    public record CheckoutInput(
            String appointmentId,
            PaymentMethod method,
            BigDecimal amountGiven,
            String couponCode
    ) {}

    public record CheckoutResult(
            Appointment appointment,
            BigDecimal change
    ) {}
}