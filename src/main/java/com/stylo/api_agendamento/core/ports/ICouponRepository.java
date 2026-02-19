package com.stylo.api_agendamento.core.ports;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;

import java.util.Optional;
import java.util.UUID;

public interface ICouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findById(UUID id);

    /**
     * Busca cupom pelo código (Case Insensitive) dentro do escopo do estabelecimento.
     */
    Optional<Coupon> findByCodeAndProviderId(String code, UUID providerId);

    /**
     * Lista cupons ativos para exibição no checkout.
     */
    PagedResult<Coupon> findAllActiveByProviderId(UUID providerId, int page, int size);
}