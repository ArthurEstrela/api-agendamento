package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.ICouponRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class ApplyCouponUseCase {

    private final ICouponRepository couponRepository;

    public CouponResult validateAndCalculate(String code, String providerId, BigDecimal purchaseAmount) {
        if (code == null || code.isBlank()) return new CouponResult(null, BigDecimal.ZERO);

        Coupon coupon = couponRepository.findByCodeAndProvider(code.toUpperCase(), providerId)
                .orElseThrow(() -> new EntityNotFoundException("Cupom inválido."));

        // Valida as regras (data, valor minimo, etc)
        coupon.validate(purchaseAmount);

        BigDecimal discount = coupon.calculateDiscount(purchaseAmount);

        return new CouponResult(coupon, discount);
    }
    
    // DTO interno para retorno
    public record CouponResult(Coupon coupon, BigDecimal discountAmount) {}
}