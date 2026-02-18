package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import java.util.Optional;

public interface ICouponRepository {
    Coupon save(Coupon coupon);
    Optional<Coupon> findById(String id);
    Optional<Coupon> findByCodeAndProvider(String code, String providerId);
    // Futuro: List<Coupon> findByProviderId(String providerId);
}