package com.stylo.api_agendamento.adapters.outbound.persistence.coupon;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaCouponRepository extends JpaRepository<CouponEntity, String> {
    
    // Busca um cupom pelo código E pelo estabelecimento (isolamento de dados)
    Optional<CouponEntity> findByCodeAndProviderId(String code, String providerId);
}